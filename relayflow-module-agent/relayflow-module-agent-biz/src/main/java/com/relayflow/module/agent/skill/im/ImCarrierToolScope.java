package com.relayflow.module.agent.skill.im;

import lombok.Value;

/**
 * Bound conversation scope for IM tools during carrier inbound.
 * Model-supplied conversation IDs MUST equal {@link #conversationId}.
 */
@Value
public class ImCarrierToolScope {

    Long tenantId;
    Long conversationId;
    Long triggerUserId;
    Long botId;
}
