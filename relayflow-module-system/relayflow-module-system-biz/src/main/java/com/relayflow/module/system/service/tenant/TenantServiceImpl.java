package com.relayflow.module.system.service.tenant;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.mysql.SysTenantMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final SysTenantMapper tenantMapper;
    private final TenantProperties tenantProperties;

    @Override
    public SysTenantDO getTenant(Long tenantId) {
        SysTenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException(ErrorCodeConstants.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    @Override
    public SysTenantDO getDefaultTenant() {
        return getTenant(tenantProperties.getDefaultId());
    }

    @Override
    public void assertDeletable(Long tenantId) {
        if (tenantId != null && tenantId == tenantProperties.getDefaultId()) {
            throw new ServiceException(ErrorCodeConstants.TENANT_DEFAULT_DELETE_FORBIDDEN);
        }
    }
}
