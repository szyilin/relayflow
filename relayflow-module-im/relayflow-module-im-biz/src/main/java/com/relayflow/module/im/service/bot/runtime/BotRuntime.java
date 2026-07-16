package com.relayflow.module.im.service.bot.runtime;

/**
 * Dispatches inbound bot events by catalog {@code handler_kind}.
 * Implementation lives in im-biz; promote to im-api only when cross-module handlers are required.
 */
public interface BotRuntime {

    /**
     * Best-effort dispatch. Failures must not roll back the triggering user message.
     */
    void dispatch(BotInboundContext context);
}
