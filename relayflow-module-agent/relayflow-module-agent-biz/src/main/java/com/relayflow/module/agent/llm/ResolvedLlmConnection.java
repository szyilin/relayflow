package com.relayflow.module.agent.llm;

import lombok.Builder;
import lombok.Value;

/**
 * Resolved tenant LLM connection credentials for runtime use (never expose to clients).
 */
@Value
@Builder
public class ResolvedLlmConnection {

    String code;
    String name;
    String providerType;
    String baseUrl;
    String apiKey;
    String modelName;
}
