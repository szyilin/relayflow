package com.relayflow.module.agent.runtime;

import lombok.Builder;
import lombok.Value;

/**
 * Internal stream events aligned with workspace-agent-chat contract (subset).
 */
@Value
@Builder
public class AgentStreamEvent {

    public static final String REASONING_DELTA = "reasoning-delta";
    public static final String REASONING_END = "reasoning-end";
    public static final String TOOL_START = "tool-start";
    public static final String TOOL_RESULT = "tool-result";
    public static final String TEXT_DELTA = "text-delta";
    public static final String DONE = "done";
    public static final String ERROR = "error";

    String type;
    String text;
    String toolCallId;
    String toolName;
    String input;
    String output;
    String message;
    Integer code;

    public static AgentStreamEvent textDelta(String text) {
        return AgentStreamEvent.builder().type(TEXT_DELTA).text(text).build();
    }

    public static AgentStreamEvent reasoningDelta(String text) {
        return AgentStreamEvent.builder().type(REASONING_DELTA).text(text).build();
    }

    public static AgentStreamEvent reasoningEnd() {
        return AgentStreamEvent.builder().type(REASONING_END).build();
    }

    public static AgentStreamEvent toolStart(String toolCallId, String toolName, String input) {
        return AgentStreamEvent.builder()
                .type(TOOL_START)
                .toolCallId(toolCallId)
                .toolName(toolName)
                .input(input)
                .build();
    }

    public static AgentStreamEvent toolResult(String toolCallId, String toolName, String output) {
        return AgentStreamEvent.builder()
                .type(TOOL_RESULT)
                .toolCallId(toolCallId)
                .toolName(toolName)
                .output(output)
                .build();
    }

    public static AgentStreamEvent done() {
        return AgentStreamEvent.builder().type(DONE).build();
    }

    public static AgentStreamEvent error(String message, Integer code) {
        return AgentStreamEvent.builder().type(ERROR).message(message).code(code).build();
    }
}
