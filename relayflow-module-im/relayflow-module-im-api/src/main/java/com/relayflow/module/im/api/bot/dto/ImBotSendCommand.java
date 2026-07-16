package com.relayflow.module.im.api.bot.dto;

import com.relayflow.module.im.api.bot.dto.card.ImBotCardDocument;
import lombok.Data;

@Data
public class ImBotSendCommand {

    /** Stable bot code, e.g. {@code org-assistant}, {@code task-bot}. */
    private String botCode;

    /**
     * Plain text body. Required when {@link #card} is null; ignored when card is present
     * (message type becomes {@code card}).
     */
    private String text;

    /**
     * Interactive card document ({@code generic.v1}). When set, persists as {@code type=card}.
     */
    private ImBotCardDocument card;

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
