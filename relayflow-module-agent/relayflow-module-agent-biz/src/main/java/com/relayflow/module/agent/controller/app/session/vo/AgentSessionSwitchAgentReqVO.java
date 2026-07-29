package com.relayflow.module.agent.controller.app.session.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentSessionSwitchAgentReqVO {

    @NotNull
    private Long sessionId;

    @NotNull
    private Long agentId;
}
