package com.relayflow.module.system.service.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.util.MobileUtils;
import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.TokenRevocationStore;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.TenantSelectionDataVO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.permission.PermissionCacheEvictor;
import com.relayflow.module.system.service.tenant.TenantService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.relayflow.framework.security.core.IdentityValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ADMIN_USER_TYPE = "admin";

    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final TenantService tenantService;
    private final TenantProperties tenantProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final ObjectProvider<TokenRevocationStore> tokenRevocationStore;
    private final PermissionCacheEvictor permissionCacheEvictor;

    @Override
    public AuthLoginRespVO login(AuthLoginReqVO request) {
        SysUserDO user = resolveUser(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }

        if (tenantProperties.isEnabled()) {
            return loginWithTenantSelection(user, request.getTenantId());
        }
        return loginSingleTenant(user);
    }

    @Override
    public void logout(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        Claims claims;
        try {
            claims = jwtTokenService.parseClaims(accessToken);
            if (!StringUtils.hasText(claims.getId()) || claims.getExpiration() == null
                    || claims.get(LoginUser.CLAIM_TENANT_ID, Long.class) == null) {
                return;
            }
        } catch (JwtException | IllegalArgumentException ex) {
            return; // Invalid/expired tokens have nothing left to revoke. Never log bearer/parser data.
        }
        Duration ttl = jwtTokenService.remainingLifetime(claims);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            TokenRevocationStore store = tokenRevocationStore.getIfAvailable();
            if (store == null) {
                throw IdentityValidationException.unavailable();
            }
            store.revoke(claims.getId(), claims.get(LoginUser.CLAIM_TENANT_ID, Long.class), ttl);
        } catch (RuntimeException ex) {
            throw IdentityValidationException.unavailable();
        }
    }

    private AuthLoginRespVO loginSingleTenant(SysUserDO user) {
        SysTenantUserDO tenantUser = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getUserId, user.getId())
                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE)
                .last("LIMIT 1"));
        if (tenantUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_USER_DISABLED);
        }
        return issueToken(user, tenantUser.getTenantId());
    }

    private AuthLoginRespVO loginWithTenantSelection(SysUserDO user, Long requestedTenantId) {
        List<SysTenantUserDO> memberships = tenantUserMapper.selectList(Wrappers.<SysTenantUserDO>lambdaQuery()
                        .eq(SysTenantUserDO::getUserId, user.getId())
                        .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE))
                .stream()
                .sorted(Comparator.comparing(SysTenantUserDO::getTenantId))
                .toList();

        if (memberships.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.AUTH_NO_TENANT);
        }

        if (memberships.size() == 1) {
            return issueToken(user, memberships.get(0).getTenantId());
        }

        if (requestedTenantId == null) {
            TenantSelectionDataVO data = new TenantSelectionDataVO();
            data.setTenants(tenantService.listMyTenants(user.getId()));
            throw new ServiceException(ErrorCodeConstants.TENANT_SELECTION_REQUIRED, data);
        }

        boolean allowed = memberships.stream()
                .anyMatch(membership -> requestedTenantId.equals(membership.getTenantId()));
        if (!allowed) {
            throw new ServiceException(ErrorCodeConstants.TENANT_SWITCH_FORBIDDEN);
        }

        return issueToken(user, requestedTenantId);
    }

    private AuthLoginRespVO issueToken(SysUserDO user, Long tenantId) {
        TenantContextHolder.set(tenantId);
        // Drop stale Redis permission snapshot so login always sees current role resolution
        // (e.g. builtin super_admin expands to newly seeded permissions).
        permissionCacheEvictor.evictUser(tenantId, user.getId());
        String accessToken = jwtTokenService.createAccessToken(
                user.getId(), user.getUsername(), tenantId, ADMIN_USER_TYPE);

        AuthLoginRespVO response = new AuthLoginRespVO();
        response.setAccessToken(accessToken);
        response.setTenantId(tenantId);
        return response;
    }

    private SysUserDO resolveUser(String loginName) {
        if (!StringUtils.hasText(loginName)) {
            return null;
        }
        String normalized = MobileUtils.normalize(loginName);
        if (!MobileUtils.isValid(normalized)) {
            return null;
        }
        SysUserDO user = userMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getMobile, normalized));
        if (user != null) {
            return user;
        }
        return userMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUsername, normalized));
    }
}
