package com.relayflow.module.agent.controller.app.session.vo;

import lombok.Data;

@Data
public class AgentSessionRespVO {

    private Long id;
    private Long agentId;
    private String agentName;
    private String title;
    private String status;
}
