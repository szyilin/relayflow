package com.relayflow.module.im.service.bot.runtime;

/**
 * In-process platform handler keyed by {@link #botCode()}.
 * Registered as Spring beans and indexed by {@link BotPlatformHandlerRegistry}.
 */
public interface BotPlatformHandler {

    String botCode();

    BotHandlerReply handle(BotInboundContext context);
}
