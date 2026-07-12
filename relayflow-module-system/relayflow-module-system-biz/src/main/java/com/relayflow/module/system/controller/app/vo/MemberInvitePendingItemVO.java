package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MemberInvitePendingItemVO {

    private Long tenantId;
    private String tenantName;
    private OffsetDateTime invitedAt;
}
