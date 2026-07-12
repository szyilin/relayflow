package com.relayflow.module.system.service.tenant;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.module.system.dal.dataobject.SysPermissionDO;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;
import com.relayflow.module.system.dal.dataobject.SysRolePermissionDO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.dataobject.SysUserRoleDO;
import com.relayflow.module.system.dal.mysql.SysPermissionMapper;
import com.relayflow.module.system.dal.mysql.SysRoleMapper;
import com.relayflow.module.system.dal.mysql.SysRolePermissionMapper;
import com.relayflow.module.system.dal.mysql.SysTenantMapper;
import com.relayflow.module.system.dal.mysql.SysUserDeptMapper;
import com.relayflow.module.system.dal.mysql.SysUserRoleMapper;
import com.relayflow.module.system.enums.DataScope;
import com.relayflow.module.system.enums.RoleType;
import com.relayflow.module.system.service.dept.DeptService;
import com.relayflow.module.system.service.permission.PermissionCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenantBootstrapServiceImpl implements TenantBootstrapService {

    private static final String SUPER_ADMIN_CODE = "super_admin";

    private final SysTenantMapper tenantMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final DeptService deptService;
    private final TenantProperties tenantProperties;
    private final PermissionCacheEvictor permissionCacheEvictor;

    @Override
    @Transactional
    public void bootstrapOwner(Long tenantId, Long ownerUserId) {
        SysTenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Owner user id is required");
        }

        SysTenantDO tenantUpdate = new SysTenantDO();
        tenantUpdate.setId(tenantId);
        tenantUpdate.setOwnerUserId(ownerUserId);
        tenantMapper.updateById(tenantUpdate);

        Long rootDeptId = deptService.getOrCreateRootDept(tenantId);
        bindPrimaryDept(tenantId, ownerUserId, rootDeptId);

        Long superAdminRoleId = ensureSuperAdminRole(tenantId);
        bindOwnerRole(tenantId, ownerUserId, superAdminRoleId);
        permissionCacheEvictor.evictUser(tenantId, ownerUserId);
    }

    private void bindPrimaryDept(Long tenantId, Long ownerUserId, Long rootDeptId) {
        userDeptMapper.delete(Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getTenantId, tenantId)
                .eq(SysUserDeptDO::getUserId, ownerUserId));

        SysUserDeptDO relation = new SysUserDeptDO();
        relation.setTenantId(tenantId);
        relation.setUserId(ownerUserId);
        relation.setDeptId(rootDeptId);
        relation.setPrimaryFlag(1);
        userDeptMapper.insert(relation);
    }

    private Long ensureSuperAdminRole(Long tenantId) {
        SysRoleDO existing = roleMapper.selectOne(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getTenantId, tenantId)
                .eq(SysRoleDO::getCode, SUPER_ADMIN_CODE)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }

        Map<Long, Long> permissionIdMapping = copyPermissionTemplate(tenantId);

        SysRoleDO role = new SysRoleDO();
        role.setTenantId(tenantId);
        role.setParentId(0L);
        role.setName("超级管理员");
        role.setCode(SUPER_ADMIN_CODE);
        role.setRoleType(RoleType.SYSTEM);
        role.setDataScope(DataScope.ALL);
        role.setCanDelegate(1);
        role.setSort(0);
        role.setStatus(0);
        roleMapper.insert(role);

        for (Long permissionId : permissionIdMapping.values()) {
            SysRolePermissionDO rolePermission = new SysRolePermissionDO();
            rolePermission.setTenantId(tenantId);
            rolePermission.setRoleId(role.getId());
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        }
        return role.getId();
    }

    private Map<Long, Long> copyPermissionTemplate(Long tenantId) {
        Long templateTenantId = tenantProperties.getDefaultId();
        List<SysPermissionDO> templatePermissions = permissionMapper.selectList(
                Wrappers.<SysPermissionDO>lambdaQuery()
                        .eq(SysPermissionDO::getTenantId, templateTenantId)
                        .orderByAsc(SysPermissionDO::getId));
        if (CollectionUtils.isEmpty(templatePermissions)) {
            return Map.of();
        }

        Map<Long, Long> permissionIdMapping = new HashMap<>();
        List<SysPermissionDO> remaining = new ArrayList<>(templatePermissions);
        while (!remaining.isEmpty()) {
            boolean progressed = false;
            var iterator = remaining.iterator();
            while (iterator.hasNext()) {
                SysPermissionDO template = iterator.next();
                Long parentId = template.getParentId() != null ? template.getParentId() : 0L;
                if (parentId != 0L && !permissionIdMapping.containsKey(parentId)) {
                    continue;
                }

                SysPermissionDO permission = new SysPermissionDO();
                permission.setTenantId(tenantId);
                permission.setParentId(parentId == 0L ? 0L : permissionIdMapping.get(parentId));
                permission.setName(template.getName());
                permission.setCode(template.getCode());
                permission.setType(template.getType());
                permission.setSort(template.getSort());
                permission.setStatus(template.getStatus());
                permissionMapper.insert(permission);
                permissionIdMapping.put(template.getId(), permission.getId());
                iterator.remove();
                progressed = true;
            }
            if (!progressed) {
                throw new IllegalStateException("Failed to copy permission template for tenant " + tenantId);
            }
        }
        return permissionIdMapping;
    }

    private void bindOwnerRole(Long tenantId, Long ownerUserId, Long roleId) {
        userRoleMapper.delete(Wrappers.<SysUserRoleDO>lambdaQuery()
                .eq(SysUserRoleDO::getTenantId, tenantId)
                .eq(SysUserRoleDO::getUserId, ownerUserId));

        SysUserRoleDO relation = new SysUserRoleDO();
        relation.setTenantId(tenantId);
        relation.setUserId(ownerUserId);
        relation.setRoleId(roleId);
        userRoleMapper.insert(relation);
    }
}
