package com.relayflow.module.im.service.bot.runtime;

import com.relayflow.module.im.enums.ImBotHandlerKind;
import com.relayflow.module.im.service.message.ImMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRuntimeImpl implements BotRuntime {

    private final BotPlatformHandlerRegistry platformHandlerRegistry;
    private final ImMessageService messageService;

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
                // V1 stub: never perform external HTTP.
                log.info("BotRuntime webhook stub (no HTTP): botCode={}, conversationId={}",
                        context.getBotCode(), context.getConversationId());
                return;
            }
            if (ImBotHandlerKind.PLATFORM.equals(kind)) {
                dispatchPlatform(context);
                return;
            }
            log.warn("BotRuntime unknown handler_kind={}: botCode={}", kind, context.getBotCode());
        } catch (Exception ex) {
            log.warn("BotRuntime.dispatch failed: botCode={}, conversationId={}",
                    context.getBotCode(), context.getConversationId(), ex);
        }
    }

    private void dispatchPlatform(BotInboundContext context) {
        BotPlatformHandler handler = platformHandlerRegistry.find(context.getBotCode());
        if (handler == null) {
            return;
        }
        BotHandlerReply reply = handler.handle(context);
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
