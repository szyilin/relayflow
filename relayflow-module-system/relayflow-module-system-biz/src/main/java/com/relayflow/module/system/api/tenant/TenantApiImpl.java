package com.relayflow.module.system.api.tenant;

import com.relayflow.module.system.api.tenant.dto.TenantRespDTO;
import com.relayflow.module.system.convert.TenantConvert;
import com.relayflow.module.system.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantApiImpl implements TenantApi {

    private final TenantService tenantService;

    @Override
    public TenantRespDTO getTenant(Long tenantId) {
        return TenantConvert.INSTANCE.toDto(tenantService.getTenant(tenantId));
    }

    @Override
    public TenantRespDTO getDefaultTenant() {
        return TenantConvert.INSTANCE.toDto(tenantService.getDefaultTenant());
    }
}
