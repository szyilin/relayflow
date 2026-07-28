package com.relayflow.module.agent.controller.app.automation.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentAutomationSaveReqVO {

    private Long id;

    @NotBlank
    @Size(max = 128)
    private String title;

    @NotBlank
    @Size(max = 20000)
    private String prompt;

    @NotNull
    private Long agentId;

    @NotBlank
    @Size(max = 64)
    private String cronExpr;

    /** active | disabled */
    @Size(max = 16)
    private String status;

    /** Optional; defaults to current user. */
    private Long targetUserId;
}
