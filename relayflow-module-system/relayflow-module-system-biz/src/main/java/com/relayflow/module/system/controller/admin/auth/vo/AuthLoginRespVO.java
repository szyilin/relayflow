package com.relayflow.module.system.controller.admin.auth.vo;

import lombok.Data;

@Data
public class AuthLoginRespVO {

    private String accessToken;
    private Long tenantId;
}
