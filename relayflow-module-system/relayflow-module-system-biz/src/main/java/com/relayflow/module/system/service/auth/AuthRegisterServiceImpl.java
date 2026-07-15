package com.relayflow.module.system.service.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.util.MobileUtils;
import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.infra.api.notify.NotifyInboxApi;
import com.relayflow.module.system.controller.app.vo.AuthRegisterReqVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterRespVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterTenantSummaryVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.tenant.TenantBootstrapService;
import com.relayflow.module.system.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthRegisterServiceImpl implements AuthRegisterService {

    private static final String ADMIN_USER_TYPE = "admin";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final TenantProperties tenantProperties;
    private final TenantService tenantService;
    private final TenantBootstrapService tenantBootstrapService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final NotifyInboxApi notifyInboxApi;

    @Override
    @Transactional
    public AuthRegisterRespVO register(AuthRegisterReqVO request) {
        assertOpenRegisterEnabled();

        String mobile = MobileUtils.normalize(request.getMobile());
        String password = request.getPassword().trim();
        String nickname = request.getNickname().trim();
        String tenantName = request.getTenantName().trim();
        validatePassword(password);

        SysUserDO user = userMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getMobile, mobile));
        if (user != null && hasActiveMembership(user.getId())) {
            throw new ServiceException(ErrorCodeConstants.USER_MOBILE_EXISTS);
        }

        if (user == null) {
            user = createUser(mobile, password, nickname);
        } else {
            updateInvitedUser(user, mobile, password, nickname);
        }

        notifyInboxApi.backfillUserIdByMobile(mobile, user.getId());

        SysTenantDO tenant = createTenant(tenantName, user.getId());
        TenantContextHolder.set(tenant.getId());
        createActiveMembership(tenant.getId(), user.getId());
        tenantBootstrapService.bootstrapOwner(tenant.getId(), user.getId());
        activatePendingInvites(user.getId());

        String accessToken = jwtTokenService.createAccessToken(
                user.getId(), user.getUsername(), tenant.getId(), ADMIN_USER_TYPE);

        AuthRegisterRespVO response = new AuthRegisterRespVO();
        response.setAccessToken(accessToken);
        response.setTenantId(tenant.getId());
        response.setTenants(buildTenantSummaries(user.getId()));
        return response;
    }

    private void assertOpenRegisterEnabled() {
        if (!tenantProperties.isEnabled() || !tenantProperties.isAllowOpenRegister()) {
            throw new ServiceException(ErrorCodeConstants.AUTH_REGISTER_DISABLED);
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ServiceException(ErrorCodeConstants.AUTH_REGISTER_PASSWORD_WEAK);
        }
    }

    private boolean hasActiveMembership(Long userId) {
        Long count = tenantUserMapper.selectCount(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getUserId, userId)
                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE));
        return count != null && count > 0;
    }

    private SysUserDO createUser(String mobile, String password, String nickname) {
        validateUsernameAvailable(mobile);

        SysUserDO user = new SysUserDO();
        user.setUsername(mobile);
        user.setMobile(mobile);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        userMapper.insert(user);
        return user;
    }

    private void updateInvitedUser(SysUserDO user, String mobile, String password, String nickname) {
        if (!mobile.equals(user.getUsername())) {
            validateUsernameAvailable(mobile);
            user.setUsername(mobile);
        }
        user.setMobile(mobile);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        userMapper.updateById(user);
    }

    private void validateUsernameAvailable(String username) {
        Long count = userMapper.selectCount(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUsername, username));
        if (count != null && count > 0) {
            throw new ServiceException(ErrorCodeConstants.USER_MOBILE_EXISTS);
        }
    }

    private SysTenantDO createTenant(String tenantName, Long ownerUserId) {
        SysTenantDO tenant = new SysTenantDO();
        tenant.setCode(generateTenantCode());
        tenant.setName(tenantName);
        tenant.setStatus(0);
        tenant.setOwnerUserId(ownerUserId);
        tenantMapper.insert(tenant);
        return tenant;
    }

    private void createActiveMembership(Long tenantId, Long userId) {
        SysTenantUserDO tenantUser = new SysTenantUserDO();
        tenantUser.setTenantId(tenantId);
        tenantUser.setUserId(userId);
        tenantUser.setStatus(TenantUserStatus.ACTIVE);
        tenantUserMapper.insert(tenantUser);
    }

    private void activatePendingInvites(Long userId) {
        List<SysTenantUserDO> pendingMemberships = tenantUserMapper.selectList(
                Wrappers.<SysTenantUserDO>lambdaQuery()
                        .eq(SysTenantUserDO::getUserId, userId)
                        .eq(SysTenantUserDO::getStatus, TenantUserStatus.NOT_JOINED));
        for (SysTenantUserDO tenantUser : pendingMemberships) {
            tenantUser.setStatus(TenantUserStatus.ACTIVE);
            tenantUserMapper.updateById(tenantUser);
        }
    }

    private List<AuthRegisterTenantSummaryVO> buildTenantSummaries(Long userId) {
        List<SysTenantUserDO> memberships = tenantUserMapper.selectList(
                Wrappers.<SysTenantUserDO>lambdaQuery()
                        .eq(SysTenantUserDO::getUserId, userId)
                        .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE));
        return memberships.stream()
                .sorted(Comparator.comparing(SysTenantUserDO::getTenantId))
                .map(membership -> toTenantSummary(membership, userId))
                .toList();
    }

    private AuthRegisterTenantSummaryVO toTenantSummary(SysTenantUserDO membership, Long userId) {
        SysTenantDO tenant = tenantService.getTenant(membership.getTenantId());
        AuthRegisterTenantSummaryVO summary = new AuthRegisterTenantSummaryVO();
        summary.setTenantId(tenant.getId());
        summary.setTenantName(tenant.getName());
        summary.setOwner(userId.equals(tenant.getOwnerUserId()));
        return summary;
    }

    private String generateTenantCode() {
        return "t_" + UUID.randomUUID().toString().replace("-", "");
    }
}
