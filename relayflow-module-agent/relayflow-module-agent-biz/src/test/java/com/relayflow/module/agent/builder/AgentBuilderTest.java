package com.relayflow.module.agent.builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentBuilderTest {

    private final AgentBuilder agentBuilder = new AgentBuilder();

    @Test
    void assembleSystemPrompt_includesPlatformBaseAndPersona() {
        String prompt = agentBuilder.assembleSystemPrompt("你是数据分析师。");
        assertTrue(prompt.contains("RelayFlow"));
        assertTrue(prompt.contains("# Persona"));
        assertTrue(prompt.contains("你是数据分析师。"));
    }

    @Test
    void assembleSystemPrompt_withoutPersona_stillHasBase() {
        String prompt = agentBuilder.assembleSystemPrompt("  ");
        assertTrue(prompt.contains("RelayFlow"));
        assertTrue(!prompt.contains("# Persona"));
    }
}
