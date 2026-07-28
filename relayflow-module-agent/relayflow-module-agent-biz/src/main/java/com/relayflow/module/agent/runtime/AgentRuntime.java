package com.relayflow.module.agent.runtime;

/**
 * Port for executing one Agent turn (Spring AI behind the implementation).
 */
public interface AgentRuntime {

    AgentRunResult run(AgentRunCommand command);
}
