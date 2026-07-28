package com.relayflow.module.agent.controller.app.llm.vo;

import lombok.Data;

@Data
public class LlmEnabledModelRespVO {

    private String code;
    private String name;
    private String modelName;
    private String providerType;
}
