package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class AppOrgInviteSharedRespVO {

    private String token;

    private String inviteCode;

    /** ISO-8601 */
    private String expiresAt;
}
