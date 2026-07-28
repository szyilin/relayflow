package com.relayflow.module.agent.api.carrier;

import com.relayflow.module.agent.api.carrier.dto.AgentCarrierInboundCommand;

/**
 * Called by IM Bot Runtime for carrier bots ({@code owner_agent_id} set).
 * Implementations MUST NOT write {@code agent_session*} for these turns.
 */
public interface AgentCarrierInboundApi {

    /**
     * @return assistant reply text, or null/blank to skip IM reply
     */
    String handleInbound(AgentCarrierInboundCommand command);
}
