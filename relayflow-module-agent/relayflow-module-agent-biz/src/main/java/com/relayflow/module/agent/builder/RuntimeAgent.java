package com.relayflow.module.agent.builder;

import lombok.Builder;
import lombok.Value;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Runnable agent snapshot assembled from definition + skills (grok-style Agent object).
 */
@Value
@Builder
public class RuntimeAgent {

    Long agentId;
    String name;
    String systemPrompt;
    String modelRef;
    @Builder.Default
    List<ToolCallback> tools = List.of();
}
