package com.relayflow.module.system.service.permission;

import com.relayflow.module.system.service.permission.dto.DataScopeResult;

public interface DataScopeHelper {

    DataScopeResult computeDataScope(Long userId, Long tenantId);
}
