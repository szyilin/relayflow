package com.relayflow.module.im.service.bot.runtime;

import com.relayflow.module.agent.api.carrier.AgentCarrierInboundApi;
import com.relayflow.module.agent.api.carrier.dto.AgentCarrierInboundCommand;
import com.relayflow.module.im.api.bot.dto.ImBotTypingCommand;
import com.relayflow.module.im.enums.ImBotHandlerKind;
import com.relayflow.module.im.service.bot.ImBotService;
import com.relayflow.module.im.service.message.ImMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class BotRuntimeImpl implements BotRuntime {

    private final BotPlatformHandlerRegistry platformHandlerRegistry;
    private final ImMessageService messageService;
    private final ImBotService imBotService;
    private final ObjectProvider<AgentCarrierInboundApi> agentCarrierInboundApi;

    public BotRuntimeImpl(BotPlatformHandlerRegistry platformHandlerRegistry,
                          @Lazy ImMessageService messageService,
                          ImBotService imBotService,
                          ObjectProvider<AgentCarrierInboundApi> agentCarrierInboundApi) {
        this.platformHandlerRegistry = platformHandlerRegistry;
        this.messageService = messageService;
        this.imBotService = imBotService;
        this.agentCarrierInboundApi = agentCarrierInboundApi;
    }

    @Override
    public void dispatch(BotInboundContext context) {
        if (context == null || context.getTenantId() == null || context.getConversationId() == null
                || context.getBotId() == null) {
            log.warn("BotRuntime.dispatch skipped: incomplete context");
            return;
        }

        String kind = StringUtils.hasText(context.getHandlerKind())
                ? context.getHandlerKind().trim()
                : ImBotHandlerKind.NOOP;

        try {
            if (ImBotHandlerKind.NOOP.equals(kind)) {
                return;
            }
            if (ImBotHandlerKind.WEBHOOK.equals(kind)) {
                log.info("BotRuntime webhook stub (no HTTP): botCode={}, conversationId={}",
                        context.getBotCode(), context.getConversationId());
                return;
            }
            if (ImBotHandlerKind.PLATFORM.equals(kind)) {
                withBotTyping(context, () -> dispatchPlatform(context));
                return;
            }
            log.warn("BotRuntime unknown handler_kind={}: botCode={}", kind, context.getBotCode());
        } catch (Exception ex) {
            log.warn("BotRuntime.dispatch failed: botCode={}, conversationId={}",
                    context.getBotCode(), context.getConversationId(), ex);
        }
    }

    private void withBotTyping(BotInboundContext context, Runnable action) {
        setTyping(context, true);
        try {
            action.run();
        } finally {
            setTyping(context, false);
        }
    }

    private void setTyping(BotInboundContext context, boolean typing) {
        if (!StringUtils.hasText(context.getBotCode()) || context.getConversationId() == null) {
            return;
        }
        try {
            ImBotTypingCommand command = new ImBotTypingCommand();
            command.setBotCode(context.getBotCode());
            command.setConversationId(context.getConversationId());
            command.setTyping(typing);
            imBotService.setTyping(command);
        } catch (Exception ex) {
            log.debug("Bot typing {} failed: botCode={}", typing, context.getBotCode(), ex);
        }
    }

    private void dispatchPlatform(BotInboundContext context) {
        if (context.getOwnerAgentId() != null) {
            dispatchAgentCarrier(context);
            return;
        }
        BotPlatformHandler handler = platformHandlerRegistry.find(context.getBotCode());
        if (handler == null) {
            return;
        }
        BotHandlerReply reply = handler.handle(context);
        persistReply(context, reply);
    }

    private void dispatchAgentCarrier(BotInboundContext context) {
        AgentCarrierInboundApi api = agentCarrierInboundApi.getIfAvailable();
        if (api == null) {
            log.warn("BotRuntime: AgentCarrierInboundApi missing for ownerAgentId={}", context.getOwnerAgentId());
            return;
        }
        AgentCarrierInboundCommand command = new AgentCarrierInboundCommand();
        command.setTenantId(context.getTenantId());
        command.setAgentId(context.getOwnerAgentId());
        command.setConversationId(context.getConversationId());
        command.setTriggerUserId(context.getTriggerUserId());
        command.setTriggerMessageId(context.getTriggerMessageId());
        command.setInboundText(context.getInboundText());
        String text = api.handleInbound(command);
        if (!StringUtils.hasText(text)) {
            return;
        }
        persistReply(context, BotHandlerReply.text(text.trim()));
    }

    private void persistReply(BotInboundContext context, BotHandlerReply reply) {
        if (reply == null || !reply.hasText()) {
            return;
        }
        messageService.sendBotReply(
                context.getTenantId(),
                context.getConversationId(),
                context.getBotId(),
                context.getBotName(),
                reply.getText());
    }
}
