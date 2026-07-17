package com.relayflow.module.system.api.event;

import lombok.Data;

@Data
public class MemberInvitedPayload {

    private Long invitingTenantId;
    private Long inviteeUserId;
    private String tenantName;
    private String inviterNickname;
}
