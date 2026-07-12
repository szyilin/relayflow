package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class AuthRegisterRespVO {

    private String accessToken;
    private Long tenantId;
    private List<AuthRegisterTenantSummaryVO> tenants;
}
