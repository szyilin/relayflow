package com.relayflow.module.system.api.event;

import lombok.Data;

@Data
public class TenantUserActivatedPayload {

    private Long tenantId;
    private Long userId;
}
