package com.relayflow.module.system.service.tenant;

import com.relayflow.module.system.dal.dataobject.SysTenantDO;

public interface TenantService {

    SysTenantDO getTenant(Long tenantId);

    SysTenantDO getDefaultTenant();

    void assertDeletable(Long tenantId);
}
