package com.relayflow.module.system.service.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mysql.SysTenantUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ADMIN_USER_TYPE = "admin";

    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    public AuthLoginRespVO login(AuthLoginReqVO request) {
        SysUserDO user = userMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }

        SysTenantUserDO tenantUser = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getUserId, user.getId())
                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE)
                .last("LIMIT 1"));
        if (tenantUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_USER_DISABLED);
        }

        Long tenantId = tenantUser.getTenantId();
        TenantContextHolder.set(tenantId);

        String accessToken = jwtTokenService.createAccessToken(
                user.getId(), user.getUsername(), tenantId, ADMIN_USER_TYPE);

        AuthLoginRespVO response = new AuthLoginRespVO();
        response.setAccessToken(accessToken);
        response.setTenantId(tenantId);
        return response;
    }
}
