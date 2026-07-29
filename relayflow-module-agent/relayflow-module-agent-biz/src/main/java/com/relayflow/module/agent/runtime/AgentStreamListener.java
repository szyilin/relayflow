package com.relayflow.module.agent.runtime;

/**
 * Receives incremental runtime events for one agent turn.
 * Implementations MUST tolerate late calls after cancel (no-op).
 */
@FunctionalInterface
public interface AgentStreamListener {

    void onEvent(AgentStreamEvent event);
}
