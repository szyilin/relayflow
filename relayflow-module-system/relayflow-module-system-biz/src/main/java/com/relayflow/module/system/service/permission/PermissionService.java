package com.relayflow.module.system.service.permission;

import com.relayflow.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.relayflow.module.system.service.permission.dto.RoleSimpleDTO;

import java.util.List;
import java.util.Set;

public interface PermissionService {

    Set<String> getPermissionCodes(Long userId, Long tenantId);

    List<RoleSimpleDTO> getRoleList(Long userId, Long tenantId);

    AuthPermissionInfoRespVO getPermissionInfo(Long userId, Long tenantId);
}
