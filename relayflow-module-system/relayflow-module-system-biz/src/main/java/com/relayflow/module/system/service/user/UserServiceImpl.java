package com.relayflow.module.system.service.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.api.user.dto.UserInviteReqDTO;
import com.relayflow.module.system.controller.admin.user.vo.UserGetRespVO;
import com.relayflow.module.system.controller.admin.user.vo.UserPageReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateDeptReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateRoleReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateStatusReqVO;
import com.relayflow.module.system.controller.app.vo.AppContactItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserProfileRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserProfileUpdateReqVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.convert.UserConvert;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.dataobject.SysUserRoleDO;
import com.relayflow.module.system.dal.mysql.SysDeptMapper;
import com.relayflow.module.system.dal.mysql.SysRoleMapper;
import com.relayflow.module.system.dal.mysql.SysTenantUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserDeptMapper;
import com.relayflow.module.system.dal.mysql.SysUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserRoleMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.dept.DeptService;
import com.relayflow.module.system.service.permission.DataScopeHelper;
import com.relayflow.module.system.service.permission.PermissionCacheEvictor;
import com.relayflow.module.system.service.permission.PermissionService;
import com.relayflow.module.system.service.permission.dto.DataScopeResult;
import com.relayflow.module.system.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final DeptService deptService;
    private final PasswordEncoder passwordEncoder;
    private final TenantProperties tenantProperties;
    private final DataScopeHelper dataScopeHelper;
    private final PermissionCacheEvictor permissionCacheEvictor;
    private final TenantService tenantService;
    private final PermissionService permissionService;

    @Override
    public AppUserProfileRespVO getMyProfile(Long userId, Long tenantId) {
        requireTenantUser(userId, tenantId);
        SysUserDO user = requireUser(userId);
        SysTenantDO tenant = tenantService.getTenant(tenantId);
        return buildProfileResponse(user, tenant, userId, tenantId);
    }

    @Override
    @Transactional
    public AppUserProfileRespVO updateMyProfile(Long userId, Long tenantId, AppUserProfileUpdateReqVO request) {
        requireTenantUser(userId, tenantId);
        SysUserDO user = requireUser(userId);

        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            if (!StringUtils.hasText(nickname)) {
                throw new ServiceException(ErrorCodeConstants.USER_NICKNAME_REQUIRED);
            }
            user.setNickname(nickname);
        }
        if (request.getAvatar() != null) {
            user.setAvatar(trimToNull(request.getAvatar()));
        }
        userMapper.updateById(user);

        SysTenantDO tenant = tenantService.getTenant(tenantId);
        return buildProfileResponse(user, tenant, userId, tenantId);
    }

    private AppUserProfileRespVO buildProfileResponse(
            SysUserDO user, SysTenantDO tenant, Long userId, Long tenantId) {
        AppUserProfileRespVO response = new AppUserProfileRespVO();
        response.setUserId(userId);
        response.setUsername(user.getUsername());
        response.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        response.setAvatar(user.getAvatar());
        response.setTenantId(tenantId);
        response.setTenantName(tenant.getName());
        response.setTenantVerified(false);
        response.setAdmin(!permissionService.getPermissionCodes(userId, tenantId).isEmpty());
        return response;
    }

    @Override
    @Transactional
    public Long createUser(UserCreateReqDTO request) {
        validateUsernameUnique(request.getUsername(), null);

        SysUserDO user = new SysUserDO();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname().trim() : request.getUsername().trim());
        user.setMobile(trimToNull(request.getMobile()));
        user.setEmail(trimToNull(request.getEmail()));
        userMapper.insert(user);

        Long tenantId = resolveTenantId();
        SysTenantUserDO tenantUser = new SysTenantUserDO();
        tenantUser.setTenantId(tenantId);
        tenantUser.setUserId(user.getId());
        tenantUser.setStatus(TenantUserStatus.ACTIVE);
        tenantUserMapper.insert(tenantUser);

        assignDept(tenantId, user.getId(), request.getDeptId(), true);
        assignRoles(tenantId, user.getId(), request.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional
    public Long inviteMember(UserInviteReqDTO request) {
        String mobile = trimToNull(request.getMobile());
        if (!StringUtils.hasText(mobile)) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }

        SysUserDO user = userMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getMobile, mobile));
        if (user == null) {
            user = new SysUserDO();
            user.setUsername(mobile);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            String nickname = request.getNickname() != null
                    ? request.getNickname().trim()
                    : mobile;
            user.setNickname(nickname);
            user.setMobile(mobile);
            user.setEmail(trimToNull(request.getEmail()));
            userMapper.insert(user);
        }

        Long tenantId = resolveTenantId();
        SysTenantUserDO existingMember = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getTenantId, tenantId)
                .eq(SysTenantUserDO::getUserId, user.getId()));
        if (existingMember != null) {
            throw new ServiceException(ErrorCodeConstants.USER_ALREADY_MEMBER);
        }

        SysTenantUserDO tenantUser = new SysTenantUserDO();
        tenantUser.setTenantId(tenantId);
        tenantUser.setUserId(user.getId());
        tenantUser.setStatus(TenantUserStatus.NOT_JOINED);
        tenantUserMapper.insert(tenantUser);

        assignDept(tenantId, user.getId(), request.getDeptId(), true);
        assignRoles(tenantId, user.getId(), request.getRoleIds());
        return user.getId();
    }

    @Override
    public UserGetRespVO getUser(Long id) {
        Long tenantId = resolveTenantId();
        SysUserDO user = requireUser(id);
        SysTenantUserDO tenantUser = requireTenantUser(id, tenantId);
        Long deptId = resolvePrimaryDeptId(id, tenantId);
        List<Long> roleIds = loadRoleIds(id, tenantId);
        return UserConvert.toGetVo(user, tenantUser.getStatus(), deptId, roleIds);
    }

    @Override
    public UserBasicDTO getUserBasic(Long id) {
        Long tenantId = resolveTenantId();
        SysUserDO user = requireUser(id);
        requireTenantUser(id, tenantId);
        UserBasicDTO dto = new UserBasicDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        return dto;
    }

    @Override
    @Transactional
    public void updateUser(UserUpdateReqVO request) {
        Long tenantId = resolveTenantId();
        requireTenantUser(request.getId(), tenantId);
        SysUserDO user = requireUser(request.getId());

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getMobile() != null) {
            user.setMobile(trimToNull(request.getMobile()));
        }
        if (request.getEmail() != null) {
            user.setEmail(trimToNull(request.getEmail()));
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateUserStatus(UserUpdateStatusReqVO request) {
        Long tenantId = resolveTenantId();
        SysTenantUserDO tenantUser = requireTenantUser(request.getId(), tenantId);
        tenantUser.setStatus(UserConvert.toMemberStatus(request.getStatus()));
        tenantUserMapper.updateById(tenantUser);
    }

    @Override
    @Transactional
    public void updateUserDept(UserUpdateDeptReqVO request) {
        Long tenantId = resolveTenantId();
        requireTenantUser(request.getId(), tenantId);
        assignDept(tenantId, request.getId(), request.getDeptId(), false);
    }

    @Override
    @Transactional
    public void updateUserRole(UserUpdateRoleReqVO request) {
        Long tenantId = resolveTenantId();
        requireTenantUser(request.getId(), tenantId);
        assignRoles(tenantId, request.getId(), request.getRoleIds());
    }

    @Override
    public PageResult<UserRespVO> getUserPage(UserPageReqVO request) {
        Long tenantId = resolveTenantId();
        List<SysTenantUserDO> tenantUsers = tenantUserMapper.selectList(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getTenantId, tenantId));
        if (tenantUsers.isEmpty()) {
            return PageResult.empty();
        }

        Map<Long, TenantUserStatus> statusByUserId = tenantUsers.stream()
                .collect(Collectors.toMap(SysTenantUserDO::getUserId, SysTenantUserDO::getStatus, (a, b) -> a));
        Set<Long> userIds = filterUserIdsByDataScope(statusByUserId.keySet(), tenantId);
        if (userIds.isEmpty()) {
            return PageResult.empty();
        }

        if (request.getDeptId() != null) {
            requireDept(request.getDeptId(), tenantId);
            Set<Long> deptUserIds = userDeptMapper.selectList(
                            Wrappers.<SysUserDeptDO>lambdaQuery()
                                    .eq(SysUserDeptDO::getTenantId, tenantId)
                                    .eq(SysUserDeptDO::getDeptId, request.getDeptId())
                                    .eq(SysUserDeptDO::getPrimaryFlag, 1))
                    .stream()
                    .map(SysUserDeptDO::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            userIds = userIds.stream()
                    .filter(deptUserIds::contains)
                    .collect(Collectors.toSet());
            if (userIds.isEmpty()) {
                return PageResult.empty();
            }
        }

        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;
        Page<SysUserDO> page = userMapper.selectPage(
                new Page<>(request.getPageNo(), request.getPageSize()),
                Wrappers.<SysUserDO>lambdaQuery()
                        .in(SysUserDO::getId, userIds)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(SysUserDO::getUsername, keyword)
                                .or()
                                .like(SysUserDO::getNickname, keyword))
                        .orderByDesc(SysUserDO::getCreateTime));

        List<SysUserDO> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), page.getTotal());
        }

        List<Long> pageUserIds = records.stream().map(SysUserDO::getId).toList();
        Map<Long, String> deptNameByUserId = loadPrimaryDeptNames(tenantId, pageUserIds);

        List<UserRespVO> list = records.stream()
                .map(user -> UserConvert.toVo(
                        user,
                        statusByUserId.getOrDefault(user.getId(), TenantUserStatus.ACTIVE),
                        deptNameByUserId.getOrDefault(user.getId(), null)))
                .toList();

        return PageResult.of(list, page.getTotal());
    }

    @Override
    public List<AppContactItemRespVO> listContactsByDept(Long deptId, String keyword) {
        Long tenantId = resolveTenantId();
        requireDept(deptId, tenantId);

        Set<Long> activeUserIds = tenantUserMapper.selectList(Wrappers.<SysTenantUserDO>lambdaQuery()
                        .eq(SysTenantUserDO::getTenantId, tenantId)
                        .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE))
                .stream()
                .map(SysTenantUserDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (activeUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> deptUserIds = userDeptMapper.selectList(
                        Wrappers.<SysUserDeptDO>lambdaQuery()
                                .eq(SysUserDeptDO::getTenantId, tenantId)
                                .eq(SysUserDeptDO::getDeptId, deptId)
                                .eq(SysUserDeptDO::getPrimaryFlag, 1))
                .stream()
                .map(SysUserDeptDO::getUserId)
                .filter(Objects::nonNull)
                .filter(activeUserIds::contains)
                .collect(Collectors.toSet());
        if (deptUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<SysUserDO> users = userMapper.selectList(Wrappers.<SysUserDO>lambdaQuery()
                .in(SysUserDO::getId, deptUserIds)
                .and(StringUtils.hasText(trimmedKeyword), wrapper -> wrapper
                        .like(SysUserDO::getUsername, trimmedKeyword)
                        .or()
                        .like(SysUserDO::getNickname, trimmedKeyword))
                .orderByAsc(SysUserDO::getNickname)
                .orderByAsc(SysUserDO::getUsername));

        SysDeptDO dept = requireDept(deptId, tenantId);
        String deptName = dept.getName();

        return users.stream()
                .map(user -> toContactItem(user, deptId, deptName))
                .toList();
    }

    private AppContactItemRespVO toContactItem(SysUserDO user, Long deptId, String deptName) {
        AppContactItemRespVO item = new AppContactItemRespVO();
        item.setId(user.getId());
        item.setUsername(user.getUsername());
        item.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        item.setDeptId(deptId);
        item.setDeptName(deptName);
        item.setAvatarText(resolveAvatarText(item.getNickname(), item.getUsername()));
        return item;
    }

    private String resolveAvatarText(String nickname, String username) {
        String source = StringUtils.hasText(nickname) ? nickname.trim() : username;
        if (!StringUtils.hasText(source)) {
            return "?";
        }
        return source.substring(0, 1);
    }

    private Set<Long> filterUserIdsByDataScope(Set<Long> tenantUserIds, Long tenantId) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return tenantUserIds;
        }

        DataScopeResult scope = dataScopeHelper.computeDataScope(loginUser.getUserId(), tenantId);
        if (scope.isAll()) {
            return tenantUserIds;
        }

        Set<Long> visibleIds = new HashSet<>();
        if (scope.isSelfOnly()) {
            visibleIds.add(loginUser.getUserId());
        }
        if (!scope.getDeptIds().isEmpty()) {
            List<SysUserDeptDO> relations = userDeptMapper.selectList(Wrappers.<SysUserDeptDO>lambdaQuery()
                    .eq(SysUserDeptDO::getTenantId, tenantId)
                    .in(SysUserDeptDO::getUserId, tenantUserIds)
                    .in(SysUserDeptDO::getDeptId, scope.getDeptIds()));
            relations.stream()
                    .map(SysUserDeptDO::getUserId)
                    .filter(Objects::nonNull)
                    .forEach(visibleIds::add);
        }

        return tenantUserIds.stream()
                .filter(visibleIds::contains)
                .collect(Collectors.toSet());
    }

    private void assignDept(Long tenantId, Long userId, Long deptId, boolean defaultRootIfNull) {
        userDeptMapper.delete(Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getTenantId, tenantId)
                .eq(SysUserDeptDO::getUserId, userId));

        Long resolvedDeptId = deptId;
        if (resolvedDeptId == null) {
            if (!defaultRootIfNull) {
                throw new ServiceException(ErrorCodeConstants.USER_DEPT_REQUIRED);
            }
            resolvedDeptId = deptService.getOrCreateRootDept(tenantId);
        }

        requireDept(resolvedDeptId, tenantId);
        SysUserDeptDO relation = new SysUserDeptDO();
        relation.setTenantId(tenantId);
        relation.setUserId(userId);
        relation.setDeptId(resolvedDeptId);
        relation.setPrimaryFlag(1);
        userDeptMapper.insert(relation);
    }

    private void assignRoles(Long tenantId, Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRoleDO>lambdaQuery()
                .eq(SysUserRoleDO::getTenantId, tenantId)
                .eq(SysUserRoleDO::getUserId, userId));

        if (CollectionUtils.isEmpty(roleIds)) {
            permissionCacheEvictor.evictUser(tenantId, userId);
            return;
        }

        Set<Long> uniqueRoleIds = roleIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Long roleId : uniqueRoleIds) {
            requireRole(roleId, tenantId);
            SysUserRoleDO relation = new SysUserRoleDO();
            relation.setTenantId(tenantId);
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
        permissionCacheEvictor.evictUser(tenantId, userId);
    }

    private void validateUsernameUnique(String username, Long excludeUserId) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        Long count = userMapper.selectCount(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUsername, username.trim())
                .ne(excludeUserId != null, SysUserDO::getId, excludeUserId));
        if (count != null && count > 0) {
            throw new ServiceException(ErrorCodeConstants.USER_USERNAME_EXISTS);
        }
    }

    private SysUserDO requireUser(Long id) {
        SysUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
        return user;
    }

    private SysTenantUserDO requireTenantUser(Long userId, Long tenantId) {
        SysTenantUserDO tenantUser = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getTenantId, tenantId)
                .eq(SysTenantUserDO::getUserId, userId));
        if (tenantUser == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
        return tenantUser;
    }

    private SysDeptDO requireDept(Long id, Long tenantId) {
        SysDeptDO dept = deptMapper.selectOne(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getTenantId, tenantId)
                .eq(SysDeptDO::getId, id));
        if (dept == null) {
            throw new ServiceException(ErrorCodeConstants.DEPT_NOT_FOUND);
        }
        return dept;
    }

    private SysRoleDO requireRole(Long id, Long tenantId) {
        SysRoleDO role = roleMapper.selectOne(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getTenantId, tenantId)
                .eq(SysRoleDO::getId, id));
        if (role == null) {
            throw new ServiceException(ErrorCodeConstants.ROLE_NOT_FOUND);
        }
        return role;
    }

    private Long resolvePrimaryDeptId(Long userId, Long tenantId) {
        List<SysUserDeptDO> relations = userDeptMapper.selectList(Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getTenantId, tenantId)
                .eq(SysUserDeptDO::getUserId, userId));
        if (relations.isEmpty()) {
            return null;
        }
        return relations.stream()
                .sorted(Comparator
                        .comparing(SysUserDeptDO::getPrimaryFlag, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SysUserDeptDO::getId))
                .map(SysUserDeptDO::getDeptId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<Long> loadRoleIds(Long userId, Long tenantId) {
        return userRoleMapper.selectList(Wrappers.<SysUserRoleDO>lambdaQuery()
                        .eq(SysUserRoleDO::getTenantId, tenantId)
                        .eq(SysUserRoleDO::getUserId, userId))
                .stream()
                .map(SysUserRoleDO::getRoleId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private Map<Long, String> loadPrimaryDeptNames(Long tenantId, List<Long> userIds) {
        List<SysUserDeptDO> relations = userDeptMapper.selectList(Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getTenantId, tenantId)
                .in(SysUserDeptDO::getUserId, userIds));

        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> deptIdByUserId = relations.stream()
                .sorted(Comparator
                        .comparing(SysUserDeptDO::getPrimaryFlag, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SysUserDeptDO::getId))
                .collect(Collectors.toMap(
                        SysUserDeptDO::getUserId,
                        SysUserDeptDO::getDeptId,
                        (existing, ignored) -> existing));

        Set<Long> deptIds = deptIdByUserId.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, SysDeptDO> deptById = deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDeptDO::getId, Function.identity()));

        return deptIdByUserId.entrySet().stream()
                .filter(entry -> deptById.containsKey(entry.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> deptById.get(entry.getValue()).getName()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        if (tenantProperties.isEnabled()) {
            if (tenantId == null) {
                throw new ServiceException(ErrorCodeConstants.TENANT_NOT_FOUND);
            }
            return tenantId;
        }
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }
}
