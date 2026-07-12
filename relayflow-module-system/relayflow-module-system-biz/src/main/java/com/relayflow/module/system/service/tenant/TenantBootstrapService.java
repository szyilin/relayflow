package com.relayflow.module.system.service.tenant;

/**
 * Initializes tenant-scoped org/RBAC structure for a new enterprise.
 * <p>
 * Used when a user registers and creates a tenant, and reusable for future install wizards.
 * Default seed tenant ({@code tenant_id = 1}) is established via Flyway migrations instead.
 */
public interface TenantBootstrapService {

    /**
     * Creates root department, copies permission template, binds {@code super_admin} to owner.
     *
     * @param tenantId    newly created tenant id
     * @param ownerUserId owner user id (already linked via {@code sys_tenant_user ACTIVE})
     */
    void bootstrapOwner(Long tenantId, Long ownerUserId);
}
