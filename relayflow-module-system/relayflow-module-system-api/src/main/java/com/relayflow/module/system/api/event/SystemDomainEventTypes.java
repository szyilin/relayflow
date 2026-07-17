package com.relayflow.module.system.api.event;

/**
 * System domain event type constants (payload DTOs live alongside).
 */
public final class SystemDomainEventTypes {

    public static final String TENANT_USER_ACTIVATED = "system.tenant_user.activated";
    public static final String MEMBER_INVITED = "system.member.invited";

    public static final String PRODUCER = "system";

    private SystemDomainEventTypes() {
    }
}
