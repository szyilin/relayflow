package com.relayflow.module.agent.api.carrier.dto;

import lombok.Data;

/**
 * IM inbound event for an Agent carrier bot (does not create agent_session rows).
 */
@Data
public class AgentCarrierInboundCommand {

    private Long tenantId;
    private Long agentId;
    private Long conversationId;
    private Long triggerUserId;
    private Long triggerMessageId;
    private String inboundText;
}
