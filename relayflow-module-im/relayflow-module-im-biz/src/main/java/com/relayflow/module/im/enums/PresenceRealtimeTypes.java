package com.relayflow.module.im.enums;

/**
 * Presence-domain realtime types used by IM typing indicator.
 */
public final class PresenceRealtimeTypes {

    public static final String DOMAIN = "presence";

    public static final String TYPING_START = "typing.start";
    public static final String TYPING_STOP = "typing.stop";
    public static final String TYPING_UPDATED = "typing.updated";

    private PresenceRealtimeTypes() {
    }
}
