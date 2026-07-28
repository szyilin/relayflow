package com.relayflow.module.agent.runtime;

import lombok.Builder;
import lombok.Value;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@Value
@Builder
public class AgentRunCommand {

    String modelRef;
    String systemPrompt;
    @Builder.Default
    List<Message> history = List.of();
    String userMessage;
    @Builder.Default
    List<org.springframework.ai.tool.ToolCallback> tools = List.of();
}
