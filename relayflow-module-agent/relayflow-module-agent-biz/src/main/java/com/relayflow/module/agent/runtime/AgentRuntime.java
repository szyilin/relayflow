package com.relayflow.module.agent.runtime;

/**
 * Port for executing one Agent turn (Spring AI behind the implementation).
 */
public interface AgentRuntime {

    AgentRunResult run(AgentRunCommand command);

    /**
     * Stream one turn. Emits text/tool/reasoning deltas then {@link AgentStreamEvent#DONE}.
     * Does not emit message.user / message.assistant (session layer owns persistence events).
     *
     * @param cancelFlag when true, generation SHOULD stop ASAP
     * @return final assistant text (may be partial if cancelled)
     */
    String stream(AgentRunCommand command, AgentStreamListener listener, java.util.concurrent.atomic.AtomicBoolean cancelFlag);
}
