package com.relayflow.module.im.service.message;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.controller.app.vo.SendMessageReqVO;
import com.relayflow.module.im.controller.app.vo.SendMessageRespVO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.dto.RealtimeSendContext;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ImMessageServiceImpl implements ImMessageService {

    private static final String SENDER_TYPE_USER = "user";
    private static final String SENDER_TYPE_SYSTEM = "system";
    private static final String MESSAGE_TYPE_SYSTEM = "system";

    private final ImMessageMapper messageMapper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImConversationService conversationService;
    private final ImContentHelper contentHelper;
    private final RealtimeTransportApi realtimeTransportApi;
    private final UserApi userApi;

    @Override
    public List<MessageItemRespVO> listMessages(Long conversationId, Long afterSeq) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return listMessages(loginUser.getTenantId(), loginUser.getUserId(), conversationId, afterSeq);
    }

    private List<MessageItemRespVO> listMessages(Long tenantId, Long userId, Long conversationId, Long afterSeq) {
        conversationService.requireMembership(tenantId, conversationId, userId);
        long cursor = afterSeq != null ? afterSeq : 0L;

        List<ImMessageDO> messages = messageMapper.selectList(
                Wrappers.<ImMessageDO>lambdaQuery()
                        .eq(ImMessageDO::getTenantId, tenantId)
                        .eq(ImMessageDO::getConversationId, conversationId)
                        .gt(ImMessageDO::getSeq, cursor)
                        .orderByAsc(ImMessageDO::getSeq));

        Map<Long, String> nicknameByUserId = loadSenderNicknames(messages);
        return messages.stream()
                .map(message -> toMessageItem(message, nicknameByUserId))
                .toList();
    }

    /**
     * Environment copy only (join tip, etc.). Business modules must not call this for
     * invites / tasks / approvals — use {@link com.relayflow.module.im.api.bot.ImBotApi}.
     */
    @Override
    @Transactional
    public void sendSystemMessage(Long tenantId, Long conversationId, String text) {
        if (!StringUtils.hasText(text)) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
        conversationService.lockConversation(tenantId, conversationId);

        MessageContentVO content = new MessageContentVO();
        content.setVersion(1);
        ContentBlockVO block = new ContentBlockVO();
        block.setType("text");
        block.setText(text.trim());
        content.setBlocks(List.of(block));

        long nextSeq = nextSeq(tenantId, conversationId);
        String contentJson = contentHelper.toJson(content);
        String preview = contentHelper.buildPreview(MESSAGE_TYPE_SYSTEM, content);
        OffsetDateTime now = OffsetDateTime.now();

        ImMessageDO message = new ImMessageDO();
        message.setTenantId(tenantId);
        message.setConversationId(conversationId);
        message.setSenderId(0L);
        message.setSenderType(SENDER_TYPE_SYSTEM);
        message.setType(MESSAGE_TYPE_SYSTEM);
        message.setContentJson(contentJson);
        message.setSeq(nextSeq);
        message.setCreateTime(now);
        messageMapper.insert(message);

        ImConversationDO conversation = conversationMapper.selectById(conversationId);
        conversation.setLastMsgId(message.getId());
        conversation.setLastMsgAt(now);
        conversation.setLastMsgPreview(preview);
        conversationMapper.updateById(conversation);

        incrementUnreadForAllMembers(tenantId, conversationId);

        MessageItemRespVO payload = toMessageItem(message, Map.of());
        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(ImRealtimeTypes.DOMAIN)
                .type(ImRealtimeTypes.MESSAGE_NEW)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();
        List<Long> recipients = conversationService.listMemberUserIds(tenantId, conversationId);
        if (!recipients.isEmpty()) {
            realtimeTransportApi.sendToUsers(tenantId, recipients, envelope);
        }
    }

    @Override
    @Transactional
    public SendMessageRespVO sendMessage(Long tenantId, Long userId, SendMessageReqVO request,
                                         RealtimeSendContext realtimeContext) {
        validateSendRequest(request);
        String messageType = contentHelper.normalizeMessageType(request.getType());
        contentHelper.validateUserMessage(request.getType(), request.getContent());
        contentHelper.enrichDownloadUrls(request.getContent());

        ImMessageDO existing = findByClientMsgId(tenantId, request.getClientMsgId());
        if (existing != null) {
            SendMessageRespVO response = toSendResponse(existing);
            dispatchRealtime(existing, request.getContent(), userId, tenantId, realtimeContext, true);
            return response;
        }

        Long conversationId = resolveConversationId(tenantId, userId, request);
        conversationService.requireMembership(tenantId, conversationId, userId);
        conversationService.lockConversation(tenantId, conversationId);

        long nextSeq = nextSeq(tenantId, conversationId);
        String contentJson = contentHelper.toJson(request.getContent());
        String preview = contentHelper.buildPreview(messageType, request.getContent());
        OffsetDateTime now = OffsetDateTime.now();

        ImMessageDO message = new ImMessageDO();
        message.setTenantId(tenantId);
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setSenderType(SENDER_TYPE_USER);
        message.setType(messageType);
        message.setContentJson(contentJson);
        message.setClientMsgId(request.getClientMsgId().trim());
        message.setSeq(nextSeq);
        message.setCreator(userId);
        message.setCreateTime(now);

        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException ex) {
            ImMessageDO raced = findByClientMsgId(tenantId, request.getClientMsgId());
            if (raced == null) {
                throw ex;
            }
            SendMessageRespVO response = toSendResponse(raced);
            dispatchRealtime(raced, request.getContent(), userId, tenantId, realtimeContext, true);
            return response;
        }

        ImConversationDO conversation = conversationMapper.selectById(conversationId);
        conversation.setLastMsgId(message.getId());
        conversation.setLastMsgAt(now);
        conversation.setLastMsgPreview(preview);
        conversation.setUpdater(userId);
        conversationMapper.updateById(conversation);

        incrementUnreadForOthers(tenantId, conversationId, userId);

        SendMessageRespVO response = toSendResponse(message);
        dispatchRealtime(message, request.getContent(), userId, tenantId, realtimeContext, false);
        return response;
    }

    @Override
    @Transactional
    public SendMessageRespVO sendMyMessage(SendMessageReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return sendMessage(loginUser.getTenantId(), loginUser.getUserId(), request, null);
    }

    private void validateSendRequest(SendMessageReqVO request) {
        if (request == null || !StringUtils.hasText(request.getClientMsgId())) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_SEND_INVALID);
        }
        if (request.getConversationId() == null && request.getPeerUserId() == null) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_SEND_INVALID);
        }
    }

    private Long resolveConversationId(Long tenantId, Long userId, SendMessageReqVO request) {
        if (request.getConversationId() != null) {
            return request.getConversationId();
        }
        return conversationService.getOrCreateDirectConversation(tenantId, userId, request.getPeerUserId());
    }

    private String resolveMessageType(String type) {
        return contentHelper.normalizeMessageType(type);
    }

    private MessageItemRespVO toMessageItem(ImMessageDO message, Map<Long, String> nicknameByUserId) {
        MessageContentVO content = contentHelper.fromJson(message.getContentJson());
        contentHelper.enrichDownloadUrls(content);
        return toMessageItem(message, content, nicknameByUserId);
    }

    private ImMessageDO findByClientMsgId(Long tenantId, String clientMsgId) {
        if (!StringUtils.hasText(clientMsgId)) {
            return null;
        }
        return messageMapper.selectOne(
                Wrappers.<ImMessageDO>lambdaQuery()
                        .eq(ImMessageDO::getTenantId, tenantId)
                        .eq(ImMessageDO::getClientMsgId, clientMsgId.trim()));
    }

    private long nextSeq(Long tenantId, Long conversationId) {
        ImMessageDO latest = messageMapper.selectOne(
                Wrappers.<ImMessageDO>lambdaQuery()
                        .eq(ImMessageDO::getTenantId, tenantId)
                        .eq(ImMessageDO::getConversationId, conversationId)
                        .orderByDesc(ImMessageDO::getSeq)
                        .last("LIMIT 1"));
        return latest == null ? 1L : latest.getSeq() + 1;
    }

    private void incrementUnreadForOthers(Long tenantId, Long conversationId, Long senderId) {
        List<ImConversationMemberDO> members = conversationMemberMapper.selectList(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER));
        for (ImConversationMemberDO member : members) {
            if (Objects.equals(member.getSubjectId(), senderId)) {
                continue;
            }
            int unread = member.getUnreadCount() != null ? member.getUnreadCount() : 0;
            member.setUnreadCount(unread + 1);
            conversationMemberMapper.updateById(member);
        }
    }

    private void incrementUnreadForAllMembers(Long tenantId, Long conversationId) {
        List<ImConversationMemberDO> members = conversationMemberMapper.selectList(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER));
        for (ImConversationMemberDO member : members) {
            int unread = member.getUnreadCount() != null ? member.getUnreadCount() : 0;
            member.setUnreadCount(unread + 1);
            conversationMemberMapper.updateById(member);
        }
    }

    private void dispatchRealtime(ImMessageDO message, MessageContentVO content, Long senderId, Long tenantId,
                                  RealtimeSendContext realtimeContext, boolean idempotentReplay) {
        if (realtimeContext != null) {
            SendMessageRespVO ackPayload = toSendResponse(message);
            RealtimeEnvelopeDTO ack = RealtimeEnvelopeDTO.builder()
                    .domain(ImRealtimeTypes.DOMAIN)
                    .type(ImRealtimeTypes.MESSAGE_ACK)
                    .requestId(realtimeContext.requestId())
                    .ts(System.currentTimeMillis())
                    .payload(ackPayload)
                    .build();
            realtimeContext.sessionSender().send(realtimeContext.session(), ack);
        }

        if (idempotentReplay) {
            return;
        }

        Map<Long, String> nicknameByUserId = loadSenderNicknames(List.of(message));
        MessageContentVO enrichedContent = contentHelper.fromJson(message.getContentJson());
        contentHelper.enrichDownloadUrls(enrichedContent);
        MessageItemRespVO newMessage = toMessageItem(message, enrichedContent, nicknameByUserId);
        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(ImRealtimeTypes.DOMAIN)
                .type(ImRealtimeTypes.MESSAGE_NEW)
                .ts(System.currentTimeMillis())
                .payload(newMessage)
                .build();

        List<Long> recipients = conversationService.listOtherMemberUserIds(
                tenantId, message.getConversationId(), senderId);
        if (!recipients.isEmpty()) {
            realtimeTransportApi.sendToUsers(tenantId, recipients, envelope);
        }
    }

    private Map<Long, String> loadSenderNicknames(List<ImMessageDO> messages) {
        Map<Long, String> nicknameByUserId = new HashMap<>();
        for (ImMessageDO message : messages) {
            if (!SENDER_TYPE_USER.equals(message.getSenderType()) || message.getSenderId() == null
                    || message.getSenderId() <= 0) {
                continue;
            }
            if (nicknameByUserId.containsKey(message.getSenderId())) {
                continue;
            }
            UserBasicDTO user = userApi.getUserBasic(message.getSenderId());
            nicknameByUserId.put(message.getSenderId(), user.getNickname());
        }
        return nicknameByUserId;
    }

    private MessageItemRespVO toMessageItem(ImMessageDO message, MessageContentVO content,
                                            Map<Long, String> nicknameByUserId) {
        MessageItemRespVO item = new MessageItemRespVO();
        item.setId(message.getId());
        item.setConversationId(message.getConversationId());
        item.setSenderId(message.getSenderId());
        item.setSenderType(message.getSenderType());
        item.setType(message.getType());
        item.setContent(content);
        item.setClientMsgId(message.getClientMsgId());
        item.setSeq(message.getSeq());
        item.setCreateTime(message.getCreateTime());
        if (SENDER_TYPE_USER.equals(message.getSenderType()) && message.getSenderId() != null
                && message.getSenderId() > 0) {
            item.setSenderNickname(nicknameByUserId.get(message.getSenderId()));
        }
        return item;
    }

    private SendMessageRespVO toSendResponse(ImMessageDO message) {
        SendMessageRespVO response = new SendMessageRespVO();
        response.setId(message.getId());
        response.setConversationId(message.getConversationId());
        response.setSeq(message.getSeq());
        response.setClientMsgId(message.getClientMsgId());
        response.setCreateTime(message.getCreateTime());
        return response;
    }
}
