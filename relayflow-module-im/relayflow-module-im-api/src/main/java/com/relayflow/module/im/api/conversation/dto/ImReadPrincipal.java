package com.relayflow.module.im.api.conversation.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Server-constructed caller for {@link com.relayflow.module.im.api.conversation.ImConversationReadApi}.
 * Model-supplied conversation IDs MUST equal {@link #allowedConversationId}.
 */
@Value
@Builder
public class ImReadPrincipal {

    Long tenantId;
    /** Triggering / acting user (required for membership checks). */
    Long userId;
    /** Optional bot reading as a conversation member (group / bot_dm). */
    Long botId;
    /** Only this conversation may be read. */
    Long allowedConversationId;

    public static ImReadPrincipal of(Long tenantId, Long userId, Long botId, Long conversationId) {
        return ImReadPrincipal.builder()
                .tenantId(tenantId)
                .userId(userId)
                .botId(botId)
                .allowedConversationId(conversationId)
                .build();
    }
}
