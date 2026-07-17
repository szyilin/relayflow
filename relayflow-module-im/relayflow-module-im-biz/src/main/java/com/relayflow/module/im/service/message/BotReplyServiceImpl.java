package com.relayflow.module.im.service.message;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.enums.ImSenderType;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BotReplyServiceImpl implements BotReplyService {

    private static final String MESSAGE_TYPE_TEXT = ImContentHelper.MESSAGE_TYPE_TEXT;

    private final ImMessageMapper messageMapper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImConversationService conversationService;
    private final ImContentHelper contentHelper;
    private final RealtimeTransportApi realtimeTransportApi;

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
        message.setSenderType(ImSenderType.BOT);
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
        List<Long> recipients = conversationService.listMemberUserIds(tenantId, conversationId);
        if (!recipients.isEmpty()) {
            realtimeTransportApi.sendToUsers(tenantId, recipients, envelope);
        }
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

    private MessageItemRespVO toMessageItem(ImMessageDO message, Map<Long, String> nicknameByUserId) {
        MessageContentVO content = contentHelper.fromJson(message.getContentJson());
        contentHelper.enrichDownloadUrls(content);
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
        if (message.getSenderId() != null && message.getSenderId() > 0) {
            item.setSenderNickname(nicknameByUserId.get(message.getSenderId()));
        }
        return item;
    }
}
