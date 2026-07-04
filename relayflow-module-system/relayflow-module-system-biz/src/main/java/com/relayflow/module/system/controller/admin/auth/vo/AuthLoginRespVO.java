package com.relayflow.module.system.controller.admin.auth.vo;

public class AuthLoginRespVO {

    private String accessToken;
    private Long tenantId;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
