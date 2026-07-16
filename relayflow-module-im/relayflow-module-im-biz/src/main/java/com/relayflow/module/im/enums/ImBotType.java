package com.relayflow.module.im.enums;

/**
 * Bot catalog classification. Send reachability uses {@link #SYSTEM} vs non-system (union of subscriptions).
 */
public final class ImBotType {

    /** Platform built-in; ImBotApi.send does not require tenant/user enablement rows. */
    public static final String SYSTEM = "system";

    /** Tenant-scoped / installable path; requires tenant ∪ user subscription. */
    public static final String TENANT = "tenant";

    private ImBotType() {
    }

    public static boolean isSystem(String type) {
        return SYSTEM.equals(type);
    }
}
