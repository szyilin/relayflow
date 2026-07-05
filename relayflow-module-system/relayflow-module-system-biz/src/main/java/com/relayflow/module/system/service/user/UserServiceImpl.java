package com.relayflow.module.system.service.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.controller.admin.user.vo.UserPageReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;
import com.relayflow.module.system.convert.UserConvert;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.mysql.SysDeptMapper;
import com.relayflow.module.system.dal.mysql.SysTenantUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserDeptMapper;
import com.relayflow.module.system.dal.mysql.SysUserMapper;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final TenantProperties tenantProperties;

    @Override
    @Transactional
    public Long createUser(UserCreateReqDTO request) {
        SysUserDO user = new SysUserDO();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setMobile(request.getMobile());
        user.setEmail(request.getEmail());
        userMapper.insert(user);

        Long tenantId = resolveTenantId();
        SysTenantUserDO tenantUser = new SysTenantUserDO();
        tenantUser.setTenantId(tenantId);
        tenantUser.setUserId(user.getId());
        tenantUser.setStatus(TenantUserStatus.ACTIVE);
        tenantUserMapper.insert(tenantUser);
        return user.getId();
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
        Set<Long> userIds = statusByUserId.keySet();

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

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }
}
