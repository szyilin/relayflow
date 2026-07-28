package com.relayflow.module.agent.controller.admin.llm.vo;

import lombok.Data;

@Data
public class LlmConnectionRespVO {

    private Long id;
    private String code;
    private String name;
    private String providerType;
    private String baseUrl;
    private String modelName;
    private Boolean enabled;
    private Boolean apiKeyConfigured;
}
