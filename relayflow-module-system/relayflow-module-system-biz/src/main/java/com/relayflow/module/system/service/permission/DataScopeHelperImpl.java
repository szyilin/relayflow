package com.relayflow.module.system.service.permission;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;
import com.relayflow.module.system.dal.dataobject.SysRoleDeptDO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.dataobject.SysUserRoleDO;
import com.relayflow.module.system.dal.mapper.SysDeptMapper;
import com.relayflow.module.system.dal.mapper.SysRoleDeptMapper;
import com.relayflow.module.system.dal.mapper.SysRoleMapper;
import com.relayflow.module.system.dal.mapper.SysUserDeptMapper;
import com.relayflow.module.system.dal.mapper.SysUserRoleMapper;
import com.relayflow.module.system.enums.DataScope;
import com.relayflow.module.system.service.permission.dto.DataScopeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataScopeHelperImpl implements DataScopeHelper {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public DataScopeResult computeDataScope(Long userId, Long tenantId) {
        List<SysUserRoleDO> userRoles = userRoleMapper.selectList(
                Wrappers.<SysUserRoleDO>lambdaQuery()
                        .eq(SysUserRoleDO::getTenantId, tenantId)
                        .eq(SysUserRoleDO::getUserId, userId));
        if (CollectionUtils.isEmpty(userRoles)) {
            DataScopeResult result = DataScopeResult.empty();
            result.setSelfOnly(true);
            return result;
        }

        Set<Long> roleIds = userRoles.stream()
                .map(SysUserRoleDO::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<SysRoleDO> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.isEmpty()) {
            DataScopeResult result = DataScopeResult.empty();
            result.setSelfOnly(true);
            return result;
        }

        if (roles.stream().anyMatch(role -> role.getDataScope() == DataScope.ALL)) {
            return DataScopeResult.all();
        }

        DataScopeResult result = DataScopeResult.empty();
        Set<Long> deptIds = new HashSet<>();
        Long userDeptId = resolvePrimaryDeptId(userId, tenantId);
        Map<Long, List<Long>> childrenByParent = loadDeptChildrenMap(tenantId);

        for (SysRoleDO role : roles) {
            DataScope dataScope = role.getDataScope() != null ? role.getDataScope() : DataScope.SELF;
            switch (dataScope) {
                case DEPT -> {
                    if (userDeptId != null) {
                        deptIds.add(userDeptId);
                    } else {
                        result.setSelfOnly(true);
                    }
                }
                case DEPT_AND_CHILD -> {
                    if (userDeptId != null) {
                        deptIds.addAll(collectDeptAndChildren(userDeptId, childrenByParent));
                    } else {
                        result.setSelfOnly(true);
                    }
                }
                case CUSTOM -> deptIds.addAll(loadCustomDeptIds(tenantId, role.getId()));
                case SELF -> result.setSelfOnly(true);
                default -> {
                }
            }
        }

        result.addDeptIds(deptIds);
        if (deptIds.isEmpty() && !result.isSelfOnly()) {
            result.setSelfOnly(true);
        }
        return result;
    }

    private Long resolvePrimaryDeptId(Long userId, Long tenantId) {
        List<SysUserDeptDO> relations = userDeptMapper.selectList(
                Wrappers.<SysUserDeptDO>lambdaQuery()
                        .eq(SysUserDeptDO::getTenantId, tenantId)
                        .eq(SysUserDeptDO::getUserId, userId));
        if (relations.isEmpty()) {
            return null;
        }
        return relations.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getPrimaryFlag() != null ? b.getPrimaryFlag() : 0,
                        a.getPrimaryFlag() != null ? a.getPrimaryFlag() : 0))
                .map(SysUserDeptDO::getDeptId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Set<Long> loadCustomDeptIds(Long tenantId, Long roleId) {
        List<SysRoleDeptDO> roleDepts = roleDeptMapper.selectList(
                Wrappers.<SysRoleDeptDO>lambdaQuery()
                        .eq(SysRoleDeptDO::getTenantId, tenantId)
                        .eq(SysRoleDeptDO::getRoleId, roleId));
        if (roleDepts.isEmpty()) {
            return Collections.emptySet();
        }
        return roleDepts.stream()
                .map(SysRoleDeptDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, List<Long>> loadDeptChildrenMap(Long tenantId) {
        List<SysDeptDO> depts = deptMapper.selectList(
                Wrappers.<SysDeptDO>lambdaQuery().eq(SysDeptDO::getTenantId, tenantId));
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (SysDeptDO dept : depts) {
            Long parentId = dept.getParentId() != null ? dept.getParentId() : 0L;
            childrenByParent.computeIfAbsent(parentId, ignored -> new java.util.ArrayList<>()).add(dept.getId());
        }
        return childrenByParent;
    }

    private Set<Long> collectDeptAndChildren(Long rootDeptId, Map<Long, List<Long>> childrenByParent) {
        Set<Long> deptIds = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootDeptId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!deptIds.add(current)) {
                continue;
            }
            List<Long> children = childrenByParent.getOrDefault(current, Collections.emptyList());
            queue.addAll(children);
        }
        return deptIds;
    }
}
