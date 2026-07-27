package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class AppOrgInviteConsumeRespVO {

    private Boolean joined;

    private Boolean alreadyMember;

    private String tenantId;
}
