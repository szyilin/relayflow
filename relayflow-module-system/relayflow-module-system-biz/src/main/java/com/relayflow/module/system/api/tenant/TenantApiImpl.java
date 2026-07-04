package com.relayflow.module.system.api.tenant;

import com.relayflow.module.system.api.tenant.dto.TenantRespDTO;
import com.relayflow.module.system.convert.TenantConvert;
import com.relayflow.module.system.service.tenant.TenantService;
import org.springframework.stereotype.Service;

@Service
public class TenantApiImpl implements TenantApi {

    private final TenantService tenantService;

    public TenantApiImpl(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public TenantRespDTO getTenant(Long tenantId) {
        return TenantConvert.toDto(tenantService.getTenant(tenantId));
    }

    @Override
    public TenantRespDTO getDefaultTenant() {
        return TenantConvert.toDto(tenantService.getDefaultTenant());
    }
}
