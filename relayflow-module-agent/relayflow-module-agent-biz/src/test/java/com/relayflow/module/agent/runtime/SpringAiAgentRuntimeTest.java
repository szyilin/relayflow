package com.relayflow.module.agent.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringAiAgentRuntimeTest {

    @Test
    void toOpenAiBaseUrl_stripsV1Suffix() {
        assertEquals("https://api.openai.com",
                SpringAiAgentRuntime.toOpenAiBaseUrl("https://api.openai.com/v1"));
        assertEquals("https://api.openai.com",
                SpringAiAgentRuntime.toOpenAiBaseUrl("https://api.openai.com/v1/"));
        assertEquals("https://api.deepseek.com",
                SpringAiAgentRuntime.toOpenAiBaseUrl("https://api.deepseek.com"));
    }
}
