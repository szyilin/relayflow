package com.relayflow.module.agent.controller.app.session.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentSessionSendReqVO {

    @NotNull
    private Long sessionId;

    @NotBlank
    @Size(max = 32000)
    private String content;
}
