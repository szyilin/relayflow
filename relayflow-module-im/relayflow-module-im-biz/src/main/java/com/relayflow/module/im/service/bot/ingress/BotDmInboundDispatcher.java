package com.relayflow.module.im.service.bot.ingress;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.service.bot.runtime.BotInboundContext;
import com.relayflow.module.im.service.message.ImContentHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * After a bot_dm user message is persisted, invoke {@link BotIngress} for the peer bot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotDmInboundDispatcher {

    private static final int BOT_STATUS_ENABLED = 1;

    private final ImConversationMapper conversationMapper;
    private final ImBotMapper botMapper;
    private final ImContentHelper contentHelper;
    private final BotIngress botIngress;

    public void dispatchAfterUserMessage(Long tenantId, Long conversationId, Long triggerUserId,
                                         Long triggerMessageId, MessageContentVO content) {
        try {
            if (tenantId == null || conversationId == null) {
                return;
            }
            ImConversationDO conversation = conversationMapper.selectById(conversationId);
            if (conversation == null || !ImConversationType.BOT_DM.equals(conversation.getType())) {
                return;
            }
            Long botId = conversation.getBotPeerBotId();
            if (botId == null) {
                return;
            }
            ImBotDO bot = botMapper.selectById(botId);
            if (bot == null || !Objects.equals(bot.getStatus(), BOT_STATUS_ENABLED)) {
                return;
            }
            String inboundText = content == null ? null : contentHelper.buildTextPreview(content);
            botIngress.onInbound(BotInboundContext.builder()
                    .tenantId(tenantId)
                    .conversationId(conversationId)
                    .botId(bot.getId())
                    .botCode(bot.getCode())
                    .handlerKind(bot.getHandlerKind())
                    .botName(bot.getName())
                    .ownerAgentId(bot.getOwnerAgentId())
                    .triggerUserId(triggerUserId)
                    .triggerMessageId(triggerMessageId)
                    .inboundText(inboundText)
                    .build());
        } catch (Exception ex) {
            log.warn("BotDmInboundDispatcher failed: conversationId={}, triggerMessageId={}",
                    conversationId, triggerMessageId, ex);
        }
    }
}
