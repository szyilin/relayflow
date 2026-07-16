package com.relayflow.module.im.api.bot.dto;

import lombok.Data;

@Data
public class ImBotSendCommand {

    /** Stable bot code, e.g. {@code org-assistant}, {@code task-bot}. */
    private String botCode;

    /** Plain text body (required for foundation text delivery). */
    private String text;

    /**
     * Optional idempotency key within {@code (tenant_id, bot_id, user_id, dedupeKey)}.
     */
    private String dedupeKey;

    private ImBotSendTarget target;

    /** Optional deep-link metadata for clients (foundation text + link). */
    private String route;
    private String entityType;
    private String entityId;
}
