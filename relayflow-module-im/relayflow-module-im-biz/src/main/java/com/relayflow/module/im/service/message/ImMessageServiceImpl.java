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
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.dal.dataobject.ImGroupDO;
import com.relayflow.module.im.dal.mapper.ImGroupMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberRole;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.enums.ImSenderType;
import com.relayflow.module.im.config.ImBotCarrierProperties;
import com.relayflow.module.im.controller.app.vo.MessageReplyToRespVO;
import com.relayflow.module.im.service.bot.ingress.BotDmInboundDispatcher;
import com.relayflow.module.im.service.bot.ingress.BotInboundAsyncDispatcher;
import com.relayflow.module.im.service.bot.ingress.GroupBotMentionDispatcher;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.dto.RealtimeSendContext;
import com.relayflow.module.im.service.productsettings.ImProductSettingsService;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImMessageServiceImpl implements ImMessageService {

    private static final String SENDER_TYPE_USER = ImSenderType.USER;
    private static final String SENDER_TYPE_BOT = ImSenderType.BOT;
    private static final String SENDER_TYPE_SYSTEM = ImSenderType.SYSTEM;
    private static final String MESSAGE_TYPE_SYSTEM = "system";
    private static final String MESSAGE_TYPE_TEXT = ImContentHelper.MESSAGE_TYPE_TEXT;

    private final ImMessageMapper messageMapper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImGroupMapper groupMapper;
    private final ImBotMapper botMapper;
    private final ImConversationService conversationService;
    private final ImProductSettingsService productSettingsService;
    private final ImContentHelper contentHelper;
    private final RealtimeTransportApi realtimeTransportApi;
    private final UserApi userApi;
    private final GroupBotMentionDispatcher groupBotMentionDispatcher;
    private final BotDmInboundDispatcher botDmInboundDispatcher;
    private final BotInboundAsyncDispatcher botInboundAsyncDispatcher;
    private final ImBotCarrierProperties botCarrierProperties;

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
        Map<Long, MessageReplyToRespVO> replyToByMsgId = loadReplyToMap(tenantId, messages, nicknameByUserId);
        return messages.stream()
                .map(message -> {
                    MessageContentVO content = contentHelper.fromJson(message.getContentJson());
                    contentHelper.enrichDownloadUrls(content);
                    return toMessageItem(message, content, nicknameByUserId, replyToByMsgId.get(message.getId()));
                })
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
    public void sendBotReply(Long tenantId, Long conversationId, Long botId, String botName, String text) {
        if (botId == null || !StringUtils.hasText(text)) {
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
        String preview = contentHelper.buildPreview(MESSAGE_TYPE_TEXT, content);
        OffsetDateTime now = OffsetDateTime.now();

        ImMessageDO message = new ImMessageDO();
        message.setTenantId(tenantId);
        message.setConversationId(conversationId);
        message.setSenderId(botId);
        message.setSenderType(SENDER_TYPE_BOT);
        message.setType(MESSAGE_TYPE_TEXT);
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

        Map<Long, String> nicknames = new HashMap<>();
        if (StringUtils.hasText(botName)) {
            nicknames.put(botId, botName);
        }
        MessageItemRespVO payload = toMessageItem(message, nicknames);
        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(ImRealtimeTypes.DOMAIN)
                .type(ImRealtimeTypes.MESSAGE_NEW)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();
        // Fanout only to User members — bots have no login client.
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
        message.setReplyToMsgId(resolveReplyToMsgId(tenantId, conversationId, request.getReplyToMsgId()));
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

        incrementUnreadForOthers(tenantId, conversationId, userId, request.getContent());

        SendMessageRespVO response = toSendResponse(message);
        dispatchRealtime(message, request.getContent(), userId, tenantId, realtimeContext, false);
        scheduleGroupBotMentionDispatch(tenantId, conversationId, userId, message.getId(), request.getContent());
        scheduleBotDmInboundDispatch(tenantId, conversationId, userId, message.getId(), request.getContent());
        return response;
    }

    private void scheduleGroupBotMentionDispatch(Long tenantId, Long conversationId, Long userId,
                                                 Long messageId, MessageContentVO content) {
        botInboundAsyncDispatcher.scheduleAfterCommit(conversationId, () ->
                groupBotMentionDispatcher.dispatchAfterUserMessage(
                        tenantId, conversationId, userId, messageId, content));
    }

    private void scheduleBotDmInboundDispatch(Long tenantId, Long conversationId, Long userId,
                                              Long messageId, MessageContentVO content) {
        botInboundAsyncDispatcher.scheduleAfterCommit(conversationId, () ->
                botDmInboundDispatcher.dispatchAfterUserMessage(
                        tenantId, conversationId, userId, messageId, content));
    }

    private Long resolveReplyToMsgId(Long tenantId, Long conversationId, Long replyToMsgId) {
        if (replyToMsgId == null) {
            return null;
        }
        ImMessageDO quoted = messageMapper.selectById(replyToMsgId);
        if (quoted == null
                || !Objects.equals(quoted.getTenantId(), tenantId)
                || !Objects.equals(quoted.getConversationId(), conversationId)
                || quoted.getRecalledAt() != null) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_REPLY_INVALID);
        }
        return quoted.getId();
    }

    @Override
    @Transactional
    public SendMessageRespVO sendMyMessage(SendMessageReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return sendMessage(loginUser.getTenantId(), loginUser.getUserId(), request, null);
    }

    @Override
    @Transactional
    public void recallMessage(Long messageId) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        ImMessageDO message = requireMessage(tenantId, messageId);
        conversationService.requireMembership(tenantId, message.getConversationId(), userId);

        if (message.getRecalledAt() != null) {
            return;
        }
        if (SENDER_TYPE_SYSTEM.equals(message.getSenderType()) || MESSAGE_TYPE_SYSTEM.equals(message.getType())) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_RECALL_DENIED);
        }

        boolean isSender = SENDER_TYPE_USER.equals(message.getSenderType())
                && Objects.equals(message.getSenderId(), userId);
        boolean groupModerator = isGroupOwnerOrAdmin(tenantId, message.getConversationId(), userId);

        if (!isSender && !groupModerator) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_RECALL_DENIED);
        }
        if (isSender && !groupModerator) {
            if (!productSettingsService.resolveRecallEnabled(tenantId)) {
                throw new ServiceException(ErrorCodeConstants.MESSAGE_RECALL_DENIED);
            }
            assertWithinWindow(message.getCreateTime(),
                    productSettingsService.resolveRecallWithinHours(tenantId),
                    ErrorCodeConstants.MESSAGE_RECALL_DENIED);
        }

        OffsetDateTime now = OffsetDateTime.now();
        MessageContentVO placeholder = textContent("消息已撤回");
        message.setContentJson(contentHelper.toJson(placeholder));
        message.setRecalledAt(now);
        message.setUpdater(userId);
        messageMapper.updateById(message);

        conversationService.lockConversation(tenantId, message.getConversationId());
        ImConversationDO conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation != null && Objects.equals(conversation.getLastMsgId(), message.getId())) {
            conversation.setLastMsgPreview("消息已撤回");
            conversation.setUpdater(userId);
            conversationMapper.updateById(conversation);
        }

        fanoutMessageEvent(tenantId, message, ImRealtimeTypes.MESSAGE_RECALLED, userId);
    }

    @Override
    @Transactional
    public void editMessage(Long messageId, String content) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        ImMessageDO message = requireMessage(tenantId, messageId);
        conversationService.requireMembership(tenantId, message.getConversationId(), userId);

        if (message.getRecalledAt() != null) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_EDIT_DENIED);
        }
        if (!SENDER_TYPE_USER.equals(message.getSenderType())
                || !Objects.equals(message.getSenderId(), userId)
                || !MESSAGE_TYPE_TEXT.equals(message.getType())) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_EDIT_DENIED);
        }
        if (!productSettingsService.resolveEditEnabled(tenantId)) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_EDIT_DENIED);
        }
        assertWithinWindow(message.getCreateTime(),
                productSettingsService.resolveEditWithinHours(tenantId),
                ErrorCodeConstants.MESSAGE_EDIT_DENIED);
        if (!StringUtils.hasText(content)) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }

        OffsetDateTime now = OffsetDateTime.now();
        MessageContentVO edited = textContent(content.trim());
        message.setContentJson(contentHelper.toJson(edited));
        message.setEditedAt(now);
        message.setUpdater(userId);
        messageMapper.updateById(message);

        conversationService.lockConversation(tenantId, message.getConversationId());
        ImConversationDO conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation != null && Objects.equals(conversation.getLastMsgId(), message.getId())) {
            conversation.setLastMsgPreview(contentHelper.buildPreview(MESSAGE_TYPE_TEXT, edited));
            conversation.setUpdater(userId);
            conversationMapper.updateById(conversation);
        }

        fanoutMessageEvent(tenantId, message, ImRealtimeTypes.MESSAGE_EDITED, userId);
    }

    private ImMessageDO requireMessage(Long tenantId, Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_NOT_FOUND);
        }
        ImMessageDO message = messageMapper.selectById(messageId);
        if (message == null || !Objects.equals(message.getTenantId(), tenantId)) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_NOT_FOUND);
        }
        return message;
    }

    private void assertWithinWindow(OffsetDateTime createTime, int withinHours, ErrorCodeConstants denied) {
        if (createTime == null) {
            throw new ServiceException(denied);
        }
        long hours = Duration.between(createTime, OffsetDateTime.now()).toHours();
        if (hours >= withinHours) {
            throw new ServiceException(denied);
        }
    }

    private boolean isGroupOwnerOrAdmin(Long tenantId, Long conversationId, Long userId) {
        ImConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !ImConversationType.GROUP.equals(conversation.getType())) {
            return false;
        }
        ImGroupDO group = groupMapper.selectOne(
                Wrappers.<ImGroupDO>lambdaQuery()
                        .eq(ImGroupDO::getTenantId, tenantId)
                        .eq(ImGroupDO::getConversationId, conversationId));
        if (group == null) {
            return false;
        }
        ImConversationMemberDO membership = conversationMemberMapper.selectOne(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .eq(ImConversationMemberDO::getSubjectId, userId));
        if (membership == null) {
            return false;
        }
        return ImMemberRole.OWNER.equals(membership.getRole())
                || ImMemberRole.ADMIN.equals(membership.getRole());
    }

    private static MessageContentVO textContent(String text) {
        MessageContentVO content = new MessageContentVO();
        content.setVersion(1);
        ContentBlockVO block = new ContentBlockVO();
        block.setType("text");
        block.setText(text);
        content.setBlocks(List.of(block));
        return content;
    }

    private void fanoutMessageEvent(Long tenantId, ImMessageDO message, String eventType, Long actorUserId) {
        Map<Long, String> nicknameByUserId = loadSenderNicknames(List.of(message));
        MessageItemRespVO payload = toMessageItem(message, nicknameByUserId);
        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(ImRealtimeTypes.DOMAIN)
                .type(eventType)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();
        List<Long> recipients = conversationService.listOtherMemberUserIds(
                tenantId, message.getConversationId(), actorUserId);
        // Include actor so multi-device / other tabs refresh
        List<Long> all = new java.util.ArrayList<>(recipients);
        if (actorUserId != null && !all.contains(actorUserId)) {
            all.add(actorUserId);
        }
        if (!all.isEmpty()) {
            realtimeTransportApi.sendToUsers(tenantId, all, envelope);
        }
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
        MessageReplyToRespVO replyTo = null;
        if (message.getReplyToMsgId() != null) {
            Map<Long, MessageReplyToRespVO> map = loadReplyToMap(
                    message.getTenantId(), List.of(message), nicknameByUserId);
            replyTo = map.get(message.getId());
        }
        return toMessageItem(message, content, nicknameByUserId, replyTo);
    }

    private MessageItemRespVO toMessageItem(ImMessageDO message, Map<Long, String> nicknameByUserId,
                                            MessageReplyToRespVO replyTo) {
        MessageContentVO content = contentHelper.fromJson(message.getContentJson());
        contentHelper.enrichDownloadUrls(content);
        return toMessageItem(message, content, nicknameByUserId, replyTo);
    }

    private Map<Long, MessageReplyToRespVO> loadReplyToMap(Long tenantId, List<ImMessageDO> messages,
                                                           Map<Long, String> nicknameBySenderId) {
        Set<Long> quotedIds = new HashSet<>();
        for (ImMessageDO message : messages) {
            if (message.getReplyToMsgId() != null) {
                quotedIds.add(message.getReplyToMsgId());
            }
        }
        if (quotedIds.isEmpty()) {
            return Map.of();
        }
        List<ImMessageDO> quoted = messageMapper.selectBatchIds(quotedIds);
        Map<Long, ImMessageDO> quotedById = new HashMap<>();
        for (ImMessageDO row : quoted) {
            if (row != null && Objects.equals(row.getTenantId(), tenantId)) {
                quotedById.put(row.getId(), row);
            }
        }
        Map<Long, String> names = new HashMap<>(nicknameBySenderId);
        names.putAll(loadSenderNicknames(new ArrayList<>(quotedById.values())));

        Map<Long, MessageReplyToRespVO> result = new HashMap<>();
        int previewMax = Math.max(20, botCarrierProperties.getReplyPreviewMaxChars());
        for (ImMessageDO message : messages) {
            if (message.getReplyToMsgId() == null) {
                continue;
            }
            ImMessageDO target = quotedById.get(message.getReplyToMsgId());
            if (target == null || !Objects.equals(target.getConversationId(), message.getConversationId())) {
                continue;
            }
            MessageReplyToRespVO replyTo = new MessageReplyToRespVO();
            replyTo.setMessageId(target.getId());
            replyTo.setSeq(target.getSeq());
            replyTo.setSenderType(target.getSenderType());
            replyTo.setSenderId(target.getSenderId());
            if (target.getSenderId() != null) {
                replyTo.setSenderNickname(names.get(target.getSenderId()));
            }
            if (target.getRecalledAt() != null) {
                replyTo.setPreview("消息已撤回");
            } else {
                String preview = contentHelper.buildPreview(
                        target.getType(), contentHelper.fromJson(target.getContentJson()));
                if (preview != null && preview.length() > previewMax) {
                    preview = preview.substring(0, previewMax) + "…";
                }
                replyTo.setPreview(preview);
            }
            result.put(message.getId(), replyTo);
        }
        return result;
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

    private void incrementUnreadForOthers(Long tenantId, Long conversationId, Long senderId,
                                          MessageContentVO content) {
        Set<Long> mentionedUserIds = contentHelper.extractUserMentionSubjectIds(content);
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
            if (member.getSubjectId() != null && mentionedUserIds.contains(member.getSubjectId())) {
                member.setMentionUnread(1);
            }
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
        MessageReplyToRespVO replyTo = null;
        if (message.getReplyToMsgId() != null) {
            replyTo = loadReplyToMap(tenantId, List.of(message), nicknameByUserId).get(message.getId());
        }
        MessageItemRespVO newMessage = toMessageItem(message, enrichedContent, nicknameByUserId, replyTo);
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
        Map<Long, String> nicknameBySenderId = new HashMap<>();
        Set<Long> botIds = new HashSet<>();
        for (ImMessageDO message : messages) {
            if (message.getSenderId() == null || message.getSenderId() <= 0) {
                continue;
            }
            if (SENDER_TYPE_USER.equals(message.getSenderType())) {
                if (nicknameBySenderId.containsKey(message.getSenderId())) {
                    continue;
                }
                UserBasicDTO user = userApi.getUserBasic(message.getSenderId());
                nicknameBySenderId.put(message.getSenderId(), user.getNickname());
            } else if (SENDER_TYPE_BOT.equals(message.getSenderType())) {
                botIds.add(message.getSenderId());
            }
        }
        if (!botIds.isEmpty()) {
            List<ImBotDO> bots = botMapper.selectBatchIds(botIds);
            for (ImBotDO bot : bots) {
                if (bot != null && bot.getId() != null && StringUtils.hasText(bot.getName())) {
                    nicknameBySenderId.put(bot.getId(), bot.getName());
                }
            }
        }
        return nicknameBySenderId;
    }

    private MessageItemRespVO toMessageItem(ImMessageDO message, MessageContentVO content,
                                            Map<Long, String> nicknameBySenderId) {
        return toMessageItem(message, content, nicknameBySenderId, null);
    }

    private MessageItemRespVO toMessageItem(ImMessageDO message, MessageContentVO content,
                                            Map<Long, String> nicknameBySenderId,
                                            MessageReplyToRespVO replyTo) {
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
        item.setRecalled(message.getRecalledAt() != null);
        item.setEdited(message.getEditedAt() != null);
        item.setRecalledAt(message.getRecalledAt());
        item.setEditedAt(message.getEditedAt());
        item.setReplyTo(replyTo);
        if (Boolean.TRUE.equals(item.getRecalled())) {
            item.setContent(textContent("消息已撤回"));
        }
        if (message.getSenderId() != null && message.getSenderId() > 0
                && (SENDER_TYPE_USER.equals(message.getSenderType())
                || SENDER_TYPE_BOT.equals(message.getSenderType()))) {
            item.setSenderNickname(nicknameBySenderId.get(message.getSenderId()));
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
