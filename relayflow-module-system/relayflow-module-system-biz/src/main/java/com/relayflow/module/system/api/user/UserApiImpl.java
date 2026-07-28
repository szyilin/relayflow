package com.relayflow.module.system.api.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.mapper.SysUserDeptMapper;
import com.relayflow.module.system.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserApiImpl implements UserApi {

    private final UserService userService;
    private final SysUserDeptMapper userDeptMapper;

    @Override
    public Long createUser(UserCreateReqDTO request) {
        return userService.createUser(request);
    }

    @Override
    public UserBasicDTO getUserBasic(Long userId) {
        return userService.getUserBasic(userId);
    }

    @Override
    public List<Long> listUserDeptIds(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        var query = Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getUserId, userId);
        Long tenantId = TenantContextHolder.get();
        if (tenantId != null) {
            query.eq(SysUserDeptDO::getTenantId, tenantId);
        }
        return userDeptMapper.selectList(query)
                .stream()
                .map(SysUserDeptDO::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> listUserIdsByDeptId(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return List.of();
        }
        var query = Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getDeptId, deptId);
        Long tenantId = TenantContextHolder.get();
        if (tenantId != null) {
            query.eq(SysUserDeptDO::getTenantId, tenantId);
        }
        return userDeptMapper.selectList(query)
                .stream()
                .map(SysUserDeptDO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
