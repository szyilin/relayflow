package com.relayflow.common.dal;

/**
 * Tenant-scoped entity with {@code tenant_id} column.
 */
public abstract class TenantBaseDO extends BaseDO {

    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
