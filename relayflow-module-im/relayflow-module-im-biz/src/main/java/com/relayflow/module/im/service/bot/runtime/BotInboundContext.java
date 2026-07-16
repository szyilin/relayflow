package com.relayflow.module.im.service.bot.runtime;

import lombok.Builder;
import lombok.Value;

/**
 * Inbound event delivered to Bot Runtime (group @mention or bot_dm text).
 * Card actions use a separate CardActionIngress — do not reuse this type.
 */
@Value
@Builder
public class BotInboundContext {

    Long tenantId;
    Long conversationId;
    Long botId;
    String botCode;
    String handlerKind;
    String botName;
    /** User who triggered the inbound event (may be null for synthetic tests). */
    Long triggerUserId;
    Long triggerMessageId;
    /** Optional excerpt of the triggering user message. */
    String inboundText;
}
