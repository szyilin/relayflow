package com.relayflow.module.agent.llm;

/**
 * Resolves Agent {@code modelRef} to decrypted OpenAI-compatible connection settings.
 */
public interface LlmConnectionResolver {

    /**
     * @param modelRef {@code agent_llm_connection.code}
     * @return enabled connection with decrypted API key
     */
    ResolvedLlmConnection requireEnabled(String modelRef);
}
