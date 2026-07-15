package com.relayflow.module.system.service.permission;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.system.dal.dataobject.SysUserRoleDO;
import com.relayflow.module.system.dal.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PermissionCacheEvictor {

    private final PermissionCacheService permissionCacheService;
    private final SysUserRoleMapper userRoleMapper;

    public void evictUser(Long tenantId, Long userId) {
        permissionCacheService.evictUser(tenantId, userId);
    }

    public void evictByRole(Long tenantId, Long roleId) {
        if (tenantId == null || roleId == null) {
            return;
        }
        List<SysUserRoleDO> userRoles = userRoleMapper.selectList(
                Wrappers.<SysUserRoleDO>lambdaQuery()
                        .eq(SysUserRoleDO::getTenantId, tenantId)
                        .eq(SysUserRoleDO::getRoleId, roleId));
        if (CollectionUtils.isEmpty(userRoles)) {
            return;
        }
        userRoles.stream()
                .map(SysUserRoleDO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(userId -> permissionCacheService.evictUser(tenantId, userId));
    }
}
