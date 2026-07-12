package com.relayflow.module.system.service.tenant;

import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterTenantSummaryVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;

import java.util.List;

public interface TenantService {

    SysTenantDO getTenant(Long tenantId);

    SysTenantDO getDefaultTenant();

    void assertDeletable(Long tenantId);

    List<AuthRegisterTenantSummaryVO> listMyTenants(Long userId);

    AuthLoginRespVO switchTenant(Long userId, Long tenantId);
}
