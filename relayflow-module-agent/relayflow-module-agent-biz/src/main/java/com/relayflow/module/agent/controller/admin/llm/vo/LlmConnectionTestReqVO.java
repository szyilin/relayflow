package com.relayflow.module.agent.controller.admin.llm.vo;

import lombok.Data;

@Data
public class LlmConnectionTestReqVO {

    /** Existing connection id; optional when testing unsaved form values. */
    private Long id;

    private String baseUrl;

    private String apiKey;

    private String providerType;
}
