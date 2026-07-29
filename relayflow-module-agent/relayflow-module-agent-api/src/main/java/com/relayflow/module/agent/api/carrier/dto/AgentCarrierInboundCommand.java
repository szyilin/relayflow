package com.relayflow.module.agent.api.carrier.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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
    /** Optional bot id for IM tool ACL (group / bot_dm member). */
    private Long botId;
    private String inboundText;
    /** Short IM history assembled by IM (not agent_session). */
    private List<AgentCarrierHistoryMessageDTO> history = new ArrayList<>();
}
