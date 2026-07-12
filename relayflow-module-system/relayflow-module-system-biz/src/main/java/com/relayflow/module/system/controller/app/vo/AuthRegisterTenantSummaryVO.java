package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class AuthRegisterTenantSummaryVO {

    private Long tenantId;
    private String tenantName;
    private Boolean owner;
}
