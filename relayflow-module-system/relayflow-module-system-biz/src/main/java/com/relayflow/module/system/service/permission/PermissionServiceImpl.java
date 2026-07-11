package com.relayflow.module.system.service.permission;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.relayflow.module.system.controller.admin.permission.vo.PermissionRespVO;
import com.relayflow.module.system.convert.PermissionConvert;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.dal.dataobject.SysPermissionDO;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;
import com.relayflow.module.system.dal.dataobject.SysRolePermissionDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserRoleDO;
import com.relayflow.module.system.dal.mysql.SysPermissionMapper;
import com.relayflow.module.system.dal.mysql.SysRoleMapper;
import com.relayflow.module.system.dal.mysql.SysRolePermissionMapper;
import com.relayflow.module.system.dal.mysql.SysUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserRoleMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.permission.dto.RoleSimpleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final TenantProperties tenantProperties;
    private final PermissionCacheService permissionCacheService;

    @Override
    public Set<String> getPermissionCodes(Long userId, Long tenantId) {
        return permissionCacheService.getOrLoad(tenantId, userId, () -> loadPermissionCodesFromDb(userId, tenantId));
    }

    private Set<String> loadPermissionCodesFromDb(Long userId, Long tenantId) {
        List<SysRoleDO> roles = loadRoles(userId, tenantId);
        if (roles.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> roleIds = roles.stream().map(SysRoleDO::getId).collect(Collectors.toSet());
        List<SysRolePermissionDO> rolePermissions = rolePermissionMapper.selectList(
                Wrappers.<SysRolePermissionDO>lambdaQuery()
                        .eq(SysRolePermissionDO::getTenantId, tenantId)
                        .in(SysRolePermissionDO::getRoleId, roleIds));
        if (rolePermissions.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermissionDO::getPermissionId)
                .collect(Collectors.toSet());
        List<SysPermissionDO> permissions = permissionMapper.selectBatchIds(permissionIds);
        return permissions.stream()
                .map(SysPermissionDO::getCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean isAdmin(Long userId, Long tenantId) {
        return !getPermissionCodes(userId, tenantId).isEmpty();
    }

    @Override
    public List<RoleSimpleDTO> getRoleList(Long userId, Long tenantId) {
        return loadRoles(userId, tenantId).stream()
                .sorted(Comparator.comparing(SysRoleDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysRoleDO::getId))
                .map(this::toRoleSimple)
                .toList();
    }

    @Override
    public AuthPermissionInfoRespVO getPermissionInfo(Long userId, Long tenantId) {
        SysUserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }

        AuthPermissionInfoRespVO response = new AuthPermissionInfoRespVO();
        response.setUserId(userId);
        response.setUsername(user.getUsername());
        response.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        response.setRoles(getRoleList(userId, tenantId).stream().map(role -> {
            AuthPermissionInfoRespVO.RoleSimpleVO vo = new AuthPermissionInfoRespVO.RoleSimpleVO();
            vo.setId(role.getId());
            vo.setCode(role.getCode());
            vo.setName(role.getName());
            return vo;
        }).toList());
        Set<String> permissionCodes = getPermissionCodes(userId, tenantId);
        response.setPermissions(new ArrayList<>(permissionCodes));
        response.setAdmin(!permissionCodes.isEmpty());
        return response;
    }


    @Override
    public List<PermissionRespVO> getPermissionTree() {
        Long tenantId = resolveTenantId();
        List<SysPermissionDO> permissions = permissionMapper.selectList(
                Wrappers.<SysPermissionDO>lambdaQuery()
                        .eq(SysPermissionDO::getTenantId, tenantId)
                        .orderByAsc(SysPermissionDO::getSort)
                        .orderByAsc(SysPermissionDO::getId));
        return PermissionConvert.buildTree(permissions);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }

    private List<SysRoleDO> loadRoles(Long userId, Long tenantId) {
        List<SysUserRoleDO> userRoles = userRoleMapper.selectList(
                Wrappers.<SysUserRoleDO>lambdaQuery()
                        .eq(SysUserRoleDO::getTenantId, tenantId)
                        .eq(SysUserRoleDO::getUserId, userId));
        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptyList();
        }
        Set<Long> roleIds = userRoles.stream()
                .map(SysUserRoleDO::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    private RoleSimpleDTO toRoleSimple(SysRoleDO role) {
        RoleSimpleDTO dto = new RoleSimpleDTO();
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        return dto;
    }
}
