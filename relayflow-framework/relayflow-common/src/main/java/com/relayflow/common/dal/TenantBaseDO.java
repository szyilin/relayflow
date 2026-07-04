package com.relayflow.common.dal;

import lombok.Getter;
import lombok.Setter;

/**
 * Tenant-scoped entity with {@code tenant_id} column.
 */
@Getter
@Setter
public abstract class TenantBaseDO extends BaseDO {

    private Long tenantId;
}
