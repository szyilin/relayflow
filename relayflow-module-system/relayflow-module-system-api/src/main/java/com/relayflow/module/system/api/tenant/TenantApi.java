package com.relayflow.module.system.api.tenant;

import com.relayflow.module.system.api.tenant.dto.TenantRespDTO;

public interface TenantApi {

    TenantRespDTO getTenant(Long tenantId);

    TenantRespDTO getDefaultTenant();
}
