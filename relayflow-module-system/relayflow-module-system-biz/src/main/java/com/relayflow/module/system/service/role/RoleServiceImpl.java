package com.relayflow.module.system.service.role;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.controller.admin.role.vo.RoleCreateReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RolePageReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleRespVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleUpdateReqVO;
import com.relayflow.module.system.convert.RoleConvert;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;
import com.relayflow.module.system.dal.dataobject.SysRoleDeptDO;
import com.relayflow.module.system.dal.dataobject.SysRolePermissionDO;
import com.relayflow.module.system.dal.dataobject.SysUserRoleDO;
import com.relayflow.module.system.dal.mysql.SysRoleDeptMapper;
import com.relayflow.module.system.dal.mysql.SysRoleMapper;
import com.relayflow.module.system.dal.mysql.SysRolePermissionMapper;
import com.relayflow.module.system.dal.mysql.SysUserRoleMapper;
import com.relayflow.module.system.enums.DataScope;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.RoleType;
import com.relayflow.module.system.service.permission.PermissionCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final TenantProperties tenantProperties;
    private final PermissionCacheEvictor permissionCacheEvictor;

    @Override
    public PageResult<RoleRespVO> getRolePage(RolePageReqVO request) {
        Long tenantId = resolveTenantId();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        Page<SysRoleDO> page = roleMapper.selectPage(
                new Page<>(request.getPageNo(), request.getPageSize()),
                Wrappers.<SysRoleDO>lambdaQuery()
                        .eq(SysRoleDO::getTenantId, tenantId)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(SysRoleDO::getName, keyword)
                                .or()
                                .like(SysRoleDO::getCode, keyword))
                        .orderByAsc(SysRoleDO::getSort)
                        .orderByAsc(SysRoleDO::getId));

        List<RoleRespVO> list = page.getRecords().stream()
                .map(RoleConvert::toVo)
                .toList();
        return PageResult.of(list, page.getTotal());
    }

    @Override
    public RoleRespVO getRole(Long id) {
        Long tenantId = resolveTenantId();
        SysRoleDO role = requireRole(id, tenantId);
        RoleRespVO vo = RoleConvert.toVo(role);
        vo.setPermissionIds(loadPermissionIds(id, tenantId));
        vo.setDeptIds(loadDeptIds(id, tenantId));
        return vo;
    }

    @Override
    @Transactional
    public Long createRole(RoleCreateReqVO request) {
        Long tenantId = resolveTenantId();
        validateParentExists(request.getParentId(), tenantId, null);
        validateCodeUnique(request.getCode().trim(), tenantId, null);
        validatePermissionSubset(request.getParentId(), tenantId, request.getPermissionIds());

        SysRoleDO role = new SysRoleDO();
        role.setTenantId(tenantId);
        role.setParentId(request.getParentId());
        role.setName(request.getName().trim());
        role.setCode(request.getCode().trim());
        role.setRoleType(RoleType.CUSTOM);
        role.setDataScope(request.getDataScope());
        role.setCanDelegate(request.getCanDelegate() != null ? request.getCanDelegate() : 0);
        role.setSort(request.getSort() != null ? request.getSort() : 0);
        role.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        role.setRemark(request.getRemark());
        roleMapper.insert(role);

        syncRolePermissions(role.getId(), tenantId, request.getPermissionIds());
        syncRoleDepts(role.getId(), tenantId, request.getDataScope(), request.getDeptIds());
        permissionCacheEvictor.evictByRole(tenantId, role.getId());
        return role.getId();
    }

    @Override
    @Transactional
    public void updateRole(RoleUpdateReqVO request) {
        Long tenantId = resolveTenantId();
        SysRoleDO existing = requireRole(request.getId(), tenantId);
        if (existing.getRoleType() == RoleType.SYSTEM) {
            throw new ServiceException(ErrorCodeConstants.ROLE_SYSTEM_UPDATE_FORBIDDEN);
        }

        validateParentExists(request.getParentId(), tenantId, request.getId());
        validateCodeUnique(request.getCode().trim(), tenantId, request.getId());
        validatePermissionSubset(request.getParentId(), tenantId, request.getPermissionIds());

        existing.setParentId(request.getParentId());
        existing.setName(request.getName().trim());
        existing.setCode(request.getCode().trim());
        existing.setDataScope(request.getDataScope());
        if (request.getCanDelegate() != null) {
            existing.setCanDelegate(request.getCanDelegate());
        }
        if (request.getSort() != null) {
            existing.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        existing.setRemark(request.getRemark());
        roleMapper.updateById(existing);

        syncRolePermissions(existing.getId(), tenantId, request.getPermissionIds());
        syncRoleDepts(existing.getId(), tenantId, request.getDataScope(), request.getDeptIds());
        permissionCacheEvictor.evictByRole(tenantId, existing.getId());
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Long tenantId = resolveTenantId();
        SysRoleDO role = requireRole(id, tenantId);
        if (role.getRoleType() == RoleType.SYSTEM) {
            throw new ServiceException(ErrorCodeConstants.ROLE_SYSTEM_DELETE_FORBIDDEN);
        }
        permissionCacheEvictor.evictByRole(tenantId, id);

        Long childCount = roleMapper.selectCount(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getTenantId, tenantId)
                .eq(SysRoleDO::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new ServiceException(ErrorCodeConstants.ROLE_EXISTS_CHILDREN);
        }

        Long userCount = userRoleMapper.selectCount(Wrappers.<SysUserRoleDO>lambdaQuery()
                .eq(SysUserRoleDO::getTenantId, tenantId)
                .eq(SysUserRoleDO::getRoleId, id));
        if (userCount != null && userCount > 0) {
            throw new ServiceException(ErrorCodeConstants.ROLE_EXISTS_USER);
        }

        rolePermissionMapper.delete(Wrappers.<SysRolePermissionDO>lambdaQuery()
                .eq(SysRolePermissionDO::getTenantId, tenantId)
                .eq(SysRolePermissionDO::getRoleId, id));
        roleDeptMapper.delete(Wrappers.<SysRoleDeptDO>lambdaQuery()
                .eq(SysRoleDeptDO::getTenantId, tenantId)
                .eq(SysRoleDeptDO::getRoleId, id));
        roleMapper.deleteById(id);
    }

    private SysRoleDO requireRole(Long id, Long tenantId) {
        SysRoleDO role = roleMapper.selectOne(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getId, id)
                .eq(SysRoleDO::getTenantId, tenantId));
        if (role == null) {
            throw new ServiceException(ErrorCodeConstants.ROLE_NOT_FOUND);
        }
        return role;
    }

    private void validateParentExists(Long parentId, Long tenantId, Long currentRoleId) {
        if (parentId == null || parentId == 0L) {
            return;
        }

        SysRoleDO parent = roleMapper.selectOne(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getId, parentId)
                .eq(SysRoleDO::getTenantId, tenantId));
        if (parent == null) {
            throw new ServiceException(ErrorCodeConstants.ROLE_PARENT_NOT_FOUND);
        }

        if (currentRoleId != null) {
            if (Objects.equals(currentRoleId, parentId)) {
                throw new ServiceException(ErrorCodeConstants.ROLE_PARENT_INVALID);
            }
            Map<Long, List<Long>> childrenByParent = loadRoleChildrenMap(tenantId);
            Set<Long> descendants = collectRoleAndChildren(currentRoleId, childrenByParent);
            if (descendants.contains(parentId)) {
                throw new ServiceException(ErrorCodeConstants.ROLE_PARENT_INVALID);
            }
        }
    }

    private void validateCodeUnique(String code, Long tenantId, Long excludeId) {
        SysRoleDO existing = roleMapper.selectOne(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getTenantId, tenantId)
                .eq(SysRoleDO::getCode, code));
        if (existing != null && !Objects.equals(existing.getId(), excludeId)) {
            throw new ServiceException(ErrorCodeConstants.ROLE_CODE_DUPLICATE);
        }
    }

    private void validatePermissionSubset(Long parentId, Long tenantId, List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        if (parentId == null || parentId == 0L) {
            return;
        }

        Set<Long> parentPermissionIds = new HashSet<>(loadPermissionIds(parentId, tenantId));
        for (Long permissionId : permissionIds) {
            if (!parentPermissionIds.contains(permissionId)) {
                throw new ServiceException(ErrorCodeConstants.ROLE_PERMISSION_EXCEED_PARENT);
            }
        }
    }

    private List<Long> loadPermissionIds(Long roleId, Long tenantId) {
        List<SysRolePermissionDO> relations = rolePermissionMapper.selectList(
                Wrappers.<SysRolePermissionDO>lambdaQuery()
                        .eq(SysRolePermissionDO::getTenantId, tenantId)
                        .eq(SysRolePermissionDO::getRoleId, roleId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(SysRolePermissionDO::getPermissionId)
                .sorted()
                .toList();
    }

    private List<Long> loadDeptIds(Long roleId, Long tenantId) {
        List<SysRoleDeptDO> relations = roleDeptMapper.selectList(
                Wrappers.<SysRoleDeptDO>lambdaQuery()
                        .eq(SysRoleDeptDO::getTenantId, tenantId)
                        .eq(SysRoleDeptDO::getRoleId, roleId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(SysRoleDeptDO::getDeptId)
                .sorted()
                .toList();
    }

    private void syncRolePermissions(Long roleId, Long tenantId, List<Long> permissionIds) {
        rolePermissionMapper.delete(Wrappers.<SysRolePermissionDO>lambdaQuery()
                .eq(SysRolePermissionDO::getTenantId, tenantId)
                .eq(SysRolePermissionDO::getRoleId, roleId));

        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }

        for (Long permissionId : permissionIds.stream().distinct().toList()) {
            SysRolePermissionDO relation = new SysRolePermissionDO();
            relation.setTenantId(tenantId);
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }
    }

    private void syncRoleDepts(Long roleId, Long tenantId, DataScope dataScope, List<Long> deptIds) {
        roleDeptMapper.delete(Wrappers.<SysRoleDeptDO>lambdaQuery()
                .eq(SysRoleDeptDO::getTenantId, tenantId)
                .eq(SysRoleDeptDO::getRoleId, roleId));

        if (dataScope != DataScope.CUSTOM || CollectionUtils.isEmpty(deptIds)) {
            return;
        }

        for (Long deptId : deptIds.stream().distinct().toList()) {
            SysRoleDeptDO relation = new SysRoleDeptDO();
            relation.setTenantId(tenantId);
            relation.setRoleId(roleId);
            relation.setDeptId(deptId);
            roleDeptMapper.insert(relation);
        }
    }

    private Map<Long, List<Long>> loadRoleChildrenMap(Long tenantId) {
        List<SysRoleDO> roles = roleMapper.selectList(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getTenantId, tenantId));
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (SysRoleDO role : roles) {
            Long parentId = role.getParentId() != null ? role.getParentId() : 0L;
            childrenByParent.computeIfAbsent(parentId, ignored -> new java.util.ArrayList<>()).add(role.getId());
        }
        return childrenByParent;
    }

    private Set<Long> collectRoleAndChildren(Long rootRoleId, Map<Long, List<Long>> childrenByParent) {
        Set<Long> roleIds = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootRoleId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!roleIds.add(current)) {
                continue;
            }
            queue.addAll(childrenByParent.getOrDefault(current, List.of()));
        }
        return roleIds;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }
}
