package com.relayflow.module.agent.controller.app.def.vo;

import lombok.Data;

@Data
public class AgentDefRespVO {

    private Long id;
    private String name;
    private String description;
    private String systemPrompt;
    private String modelRef;
    private String status;
    private Long ownerUserId;
    private String visibility;
}
