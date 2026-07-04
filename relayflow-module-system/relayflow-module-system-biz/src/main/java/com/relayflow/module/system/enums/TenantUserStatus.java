package com.relayflow.module.system.enums;

/**
 * Member lifecycle status within a tenant.
 */
public enum TenantUserStatus {

    NOT_JOINED,
    PENDING_ACTIVATION,
    ACTIVE,
    SUSPENDED,
    PENDING_LEAVE,
    LEFT
}
