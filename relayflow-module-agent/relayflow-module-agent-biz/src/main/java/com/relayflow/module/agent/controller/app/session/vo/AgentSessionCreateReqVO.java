package com.relayflow.module.agent.controller.app.session.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentSessionCreateReqVO {

    @NotNull
    private Long agentId;

    private String title;
}
