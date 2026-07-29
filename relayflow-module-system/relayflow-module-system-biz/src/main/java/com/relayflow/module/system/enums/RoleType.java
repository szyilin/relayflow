package com.relayflow.module.system.enums;

/**
 * Role classification.
 */
public enum RoleType {

    SYSTEM,
    CUSTOM;

    /**
     * Built-in enterprise owner role ({@code role_type=SYSTEM}).
     * Permission resolution expands to all {@code sys_permission} of the tenant at runtime —
     * do not rely on keeping {@code sys_role_permission} in sync for this role.
     */
    public static final String SUPER_ADMIN_CODE = "super_admin";
}
