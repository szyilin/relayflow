package com.relayflow.module.system.service.role;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.controller.admin.role.vo.RoleAdminSimpleVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleCreateReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleMemberRemoveReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleMembersAddReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RolePageReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleRespVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleUpdateReqVO;
import com.relayflow.module.system.convert.RoleConvert;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import com.relayflow.module.system.dal.dataobject.SysPermissionDO;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;
import com.relayflow.module.system.dal.dataobject.SysRolePermissionDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.dataobject.SysUserRoleDO;
import com.relayflow.module.system.dal.mapper.SysDeptMapper;
import com.relayflow.module.system.dal.mapper.SysPermissionMapper;
import com.relayflow.module.system.dal.mapper.SysRoleMapper;
import com.relayflow.module.system.dal.mapper.SysRolePermissionMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserDeptMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserRoleMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.RoleType;
import com.relayflow.module.system.service.permission.PermissionCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final int PAGE_ADMIN_PREVIEW_LIMIT = 8;
    private static final int PERMISSION_SUMMARY_MAX_LEN = 48;

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysDeptMapper deptMapper;
    private final SysPermissionMapper permissionMapper;
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
                        .like(StringUtils.hasText(keyword), SysRoleDO::getName, keyword)
                        .orderByAsc(SysRoleDO::getSort)
                        .orderByAsc(SysRoleDO::getId));

        List<RoleRespVO> list = page.getRecords().stream()
                .map(RoleConvert.INSTANCE::toVo)
                .toList();
        enrichPageExtras(list, tenantId);
        return PageResult.of(list, page.getTotal());
    }

    @Override
    public RoleRespVO getRole(Long id) {
        Long tenantId = resolveTenantId();
        SysRoleDO role = requireRole(id, tenantId);
        RoleRespVO vo = RoleConvert.INSTANCE.toVo(role);
        vo.setPermissionIds(resolveEffectivePermissionIds(vo, loadPermissionIds(id, tenantId), tenantId));
        vo.setPermissionSummary(buildPermissionSummary(vo.getPermissionIds(), tenantId));
        vo.setAdmins(listMembers(id));
        return vo;
    }

    @Override
    @Transactional
    public Long createRole(RoleCreateReqVO request) {
        Long tenantId = resolveTenantId();
        Long parentId = normalizeParentId(request.getParentId());
        validateParentExists(parentId, tenantId, null);
        validatePermissionSubset(parentId, tenantId, request.getPermissionIds());

        SysRoleDO role = new SysRoleDO();
        role.setTenantId(tenantId);
        role.setParentId(parentId);
        role.setName(request.getName().trim());
        role.setCode(null);
        role.setRoleType(RoleType.CUSTOM);
        role.setCanDelegate(request.getCanDelegate() != null ? request.getCanDelegate() : 0);
        role.setSort(request.getSort() != null ? request.getSort() : 0);
        role.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        role.setRemark(request.getRemark());
        roleMapper.insert(role);

        syncRolePermissions(role.getId(), tenantId, request.getPermissionIds());
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

        Long parentId = normalizeParentId(request.getParentId());
        validateParentExists(parentId, tenantId, request.getId());
        validatePermissionSubset(parentId, tenantId, request.getPermissionIds());

        existing.setParentId(parentId);
        existing.setName(request.getName().trim());
        existing.setCode(null);
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
        roleMapper.deleteById(id);
    }

    @Override
    public List<RoleAdminSimpleVO> listMembers(Long roleId) {
        Long tenantId = resolveTenantId();
        requireRole(roleId, tenantId);
        return loadAdmins(List.of(roleId), tenantId, Integer.MAX_VALUE).getOrDefault(roleId, List.of());
    }

    @Override
    @Transactional
    public void addMembers(RoleMembersAddReqVO request) {
        Long tenantId = resolveTenantId();
        requireRole(request.getRoleId(), tenantId);
        if (CollectionUtils.isEmpty(request.getUserIds())) {
            return;
        }

        Set<Long> existing = userRoleMapper.selectList(Wrappers.<SysUserRoleDO>lambdaQuery()
                        .eq(SysUserRoleDO::getTenantId, tenantId)
                        .eq(SysUserRoleDO::getRoleId, request.getRoleId())
                        .in(SysUserRoleDO::getUserId, request.getUserIds()))
                .stream()
                .map(SysUserRoleDO::getUserId)
                .collect(Collectors.toSet());

        for (Long userId : request.getUserIds().stream().distinct().toList()) {
            requireTenantMember(userId, tenantId);
            if (existing.contains(userId)) {
                continue;
            }
            SysUserRoleDO relation = new SysUserRoleDO();
            relation.setTenantId(tenantId);
            relation.setUserId(userId);
            relation.setRoleId(request.getRoleId());
            userRoleMapper.insert(relation);
            permissionCacheEvictor.evictUser(tenantId, userId);
        }
    }

    @Override
    @Transactional
    public void removeMember(RoleMemberRemoveReqVO request) {
        Long tenantId = resolveTenantId();
        requireRole(request.getRoleId(), tenantId);
        userRoleMapper.delete(Wrappers.<SysUserRoleDO>lambdaQuery()
                .eq(SysUserRoleDO::getTenantId, tenantId)
                .eq(SysUserRoleDO::getRoleId, request.getRoleId())
                .eq(SysUserRoleDO::getUserId, request.getUserId()));
        permissionCacheEvictor.evictUser(tenantId, request.getUserId());
    }

    private void enrichPageExtras(List<RoleRespVO> list, Long tenantId) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> roleIds = list.stream().map(RoleRespVO::getId).toList();
        Map<Long, List<Long>> permissionIdsByRole = loadPermissionIdsByRoles(roleIds, tenantId);
        Map<Long, List<RoleAdminSimpleVO>> adminsByRole = loadAdmins(roleIds, tenantId, PAGE_ADMIN_PREVIEW_LIMIT);
        for (RoleRespVO vo : list) {
            List<Long> permissionIds = resolveEffectivePermissionIds(
                    vo, permissionIdsByRole.getOrDefault(vo.getId(), List.of()), tenantId);
            vo.setPermissionIds(permissionIds);
            vo.setPermissionSummary(buildPermissionSummary(permissionIds, tenantId));
            vo.setAdmins(adminsByRole.getOrDefault(vo.getId(), List.of()));
        }
    }

    /**
     * Builtin {@code super_admin} is not edited via role-permission rows; display and auth
     * treat it as owning every permission defined for the tenant.
     */
    private List<Long> resolveEffectivePermissionIds(RoleRespVO vo, List<Long> boundIds, Long tenantId) {
        if (isBuiltinSuperAdmin(vo.getRoleType(), vo.getCode())) {
            return loadAllPermissionIds(tenantId);
        }
        return boundIds;
    }

    private List<Long> loadAllPermissionIds(Long tenantId) {
        return permissionMapper.selectList(Wrappers.<SysPermissionDO>lambdaQuery()
                        .eq(SysPermissionDO::getTenantId, tenantId)
                        .orderByAsc(SysPermissionDO::getId))
                .stream()
                .map(SysPermissionDO::getId)
                .toList();
    }

    private static boolean isBuiltinSuperAdmin(RoleType roleType, String code) {
        return RoleType.SYSTEM == roleType && RoleType.SUPER_ADMIN_CODE.equals(code);
    }

    private Map<Long, List<Long>> loadPermissionIdsByRoles(List<Long> roleIds, Long tenantId) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        List<SysRolePermissionDO> relations = rolePermissionMapper.selectList(
                Wrappers.<SysRolePermissionDO>lambdaQuery()
                        .eq(SysRolePermissionDO::getTenantId, tenantId)
                        .in(SysRolePermissionDO::getRoleId, roleIds));
        Map<Long, List<Long>> result = new HashMap<>();
        for (SysRolePermissionDO relation : relations) {
            result.computeIfAbsent(relation.getRoleId(), ignored -> new ArrayList<>())
                    .add(relation.getPermissionId());
        }
        result.values().forEach(ids -> ids.sort(Long::compareTo));
        return result;
    }

    private Map<Long, List<RoleAdminSimpleVO>> loadAdmins(List<Long> roleIds, Long tenantId, int limitPerRole) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserRoleDO> relations = userRoleMapper.selectList(Wrappers.<SysUserRoleDO>lambdaQuery()
                .eq(SysUserRoleDO::getTenantId, tenantId)
                .in(SysUserRoleDO::getRoleId, roleIds)
                .orderByAsc(SysUserRoleDO::getId));
        if (relations.isEmpty()) {
            return Map.of();
        }

        Set<Long> userIds = relations.stream().map(SysUserRoleDO::getUserId).collect(Collectors.toSet());
        Map<Long, SysUserDO> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUserDO::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> deptNameByUser = loadPrimaryDeptNames(userIds, tenantId);

        Map<Long, List<RoleAdminSimpleVO>> result = new HashMap<>();
        for (SysUserRoleDO relation : relations) {
            List<RoleAdminSimpleVO> bucket = result.computeIfAbsent(relation.getRoleId(), ignored -> new ArrayList<>());
            if (bucket.size() >= limitPerRole) {
                continue;
            }
            SysUserDO user = users.get(relation.getUserId());
            if (user == null) {
                continue;
            }
            RoleAdminSimpleVO admin = new RoleAdminSimpleVO();
            admin.setUserId(user.getId());
            admin.setNickname(user.getNickname());
            admin.setUsername(user.getUsername());
            admin.setAvatar(user.getAvatar());
            admin.setMobile(user.getMobile());
            admin.setDeptName(deptNameByUser.get(user.getId()));
            bucket.add(admin);
        }
        return result;
    }

    private Map<Long, String> loadPrimaryDeptNames(Set<Long> userIds, Long tenantId) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserDeptDO> relations = userDeptMapper.selectList(Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getTenantId, tenantId)
                .in(SysUserDeptDO::getUserId, userIds)
                .eq(SysUserDeptDO::getPrimaryFlag, 1));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Set<Long> deptIds = relations.stream().map(SysUserDeptDO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptNames = deptIds.isEmpty()
                ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDeptDO::getId, SysDeptDO::getName, (a, b) -> a));
        Map<Long, String> result = new HashMap<>();
        for (SysUserDeptDO relation : relations) {
            result.putIfAbsent(relation.getUserId(), deptNames.get(relation.getDeptId()));
        }
        return result;
    }

    private String buildPermissionSummary(List<Long> permissionIds, Long tenantId) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return "";
        }
        List<SysPermissionDO> all = permissionMapper.selectList(Wrappers.<SysPermissionDO>lambdaQuery()
                .eq(SysPermissionDO::getTenantId, tenantId));
        Map<Long, SysPermissionDO> byId = all.stream()
                .collect(Collectors.toMap(SysPermissionDO::getId, Function.identity(), (a, b) -> a));
        LinkedHashSet<String> roots = new LinkedHashSet<>();
        for (Long permissionId : permissionIds) {
            SysPermissionDO current = byId.get(permissionId);
            while (current != null) {
                Long parentId = current.getParentId() != null ? current.getParentId() : 0L;
                if (parentId == 0L) {
                    if (StringUtils.hasText(current.getName())) {
                        roots.add(current.getName());
                    }
                    break;
                }
                current = byId.get(parentId);
            }
        }
        if (roots.isEmpty()) {
            return "";
        }
        String joined = String.join("、", roots);
        if (joined.length() <= PERMISSION_SUMMARY_MAX_LEN) {
            return joined;
        }
        return joined.substring(0, PERMISSION_SUMMARY_MAX_LEN - 1) + "…";
    }

    private void requireTenantMember(Long userId, Long tenantId) {
        SysTenantUserDO tenantUser = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getTenantId, tenantId)
                .eq(SysTenantUserDO::getUserId, userId));
        if (tenantUser == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
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

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0L ? 0L : parentId;
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

    private Map<Long, List<Long>> loadRoleChildrenMap(Long tenantId) {
        List<SysRoleDO> roles = roleMapper.selectList(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getTenantId, tenantId));
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (SysRoleDO role : roles) {
            Long parentId = role.getParentId() != null ? role.getParentId() : 0L;
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(role.getId());
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
