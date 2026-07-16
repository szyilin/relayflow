package com.relayflow.module.im.service.bot.ingress;

import com.relayflow.module.im.service.bot.runtime.BotInboundContext;

/**
 * Entry for dialogue inbound (group @Bot / bot_dm). Separate from Card Action Ingress.
 */
public interface BotIngress {

    /**
     * Best-effort: load bot metadata if needed and dispatch to {@code BotRuntime}.
     * Must not throw to callers that already persisted the user message.
     */
    void onInbound(BotInboundContext context);
}
