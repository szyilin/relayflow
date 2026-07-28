package com.relayflow.module.agent.controller.app.automation.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AgentAutomationRespVO {

    private Long id;
    private Long agentId;
    private String agentName;
    private String title;
    private String prompt;
    private String cronExpr;
    private String status;
    private Long targetUserId;
    private OffsetDateTime nextRunAt;
    private OffsetDateTime lastRunAt;
    private String lastStatus;
    private String lastError;
}
