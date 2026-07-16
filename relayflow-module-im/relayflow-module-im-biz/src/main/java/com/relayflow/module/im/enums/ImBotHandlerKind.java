package com.relayflow.module.im.enums;

/**
 * Values for {@code im_bot.handler_kind}. Cross-module registration of platform handlers
 * stays in im-biz for V1; promote SPI to im-api only when business modules must register handlers.
 */
public final class ImBotHandlerKind {

    public static final String NOOP = "noop";
    public static final String PLATFORM = "platform";
    /** Stub only in V1 — no external HTTP. */
    public static final String WEBHOOK = "webhook";

    private ImBotHandlerKind() {
    }
}
