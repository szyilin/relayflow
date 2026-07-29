package com.relayflow.module.im.enums;

/**
 * How Bot Runtime wraps group replies with @mention of the trigger user.
 * Platform-enforced — agents MUST NOT decide this.
 */
public final class ImBotGroupReplyMentionMode {

    /** Prepend structured @trigger user mention in group replies (default). */
    public static final String TRIGGER = "trigger";

    /** Do not auto-mention (scheduled / broadcast group assistants). */
    public static final String NONE = "none";

    private ImBotGroupReplyMentionMode() {
    }
}
