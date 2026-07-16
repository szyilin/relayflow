package com.relayflow.module.system.service.memberinvite;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.MemberInvitePreviewRespVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Deprecated(since = "0.2.0")
@RequiredArgsConstructor
public class MemberInviteServiceImpl implements MemberInviteService {

    private static final String ADMIN_USER_TYPE = "admin";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final TenantService tenantService;
    private final TenantProperties tenantProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final ImBotApi imBotApi;

    @Override
    public MemberInvitePreviewRespVO preview(String mobile) {
        SysUserDO user = requireUserByMobile(mobile);
        Long tenantId = resolveTenantId();
        requirePendingInvite(user.getId(), tenantId);

        SysTenantDO tenant = tenantService.getTenant(tenantId);
        MemberInvitePreviewRespVO response = new MemberInvitePreviewRespVO();
        response.setTenantId(tenantId);
        response.setTenantName(tenant.getName());
        response.setNickname(user.getNickname());
        return response;
    }

    @Override
    @Transactional
    public AuthLoginRespVO accept(String mobile, String password) {
        validatePassword(password);

        SysUserDO user = requireUserByMobile(mobile);
        Long tenantId = resolveTenantId();
        SysTenantUserDO tenantUser = requirePendingInvite(user.getId(), tenantId);

        user.setPassword(passwordEncoder.encode(password.trim()));
        userMapper.updateById(user);

        tenantUser.setStatus(TenantUserStatus.ACTIVE);
        tenantUserMapper.updateById(tenantUser);
        imBotApi.ensureUserEnablementsOnActive(tenantId, user.getId());

        TenantContextHolder.set(tenantId);
        String accessToken = jwtTokenService.createAccessToken(
                user.getId(), user.getUsername(), tenantId, ADMIN_USER_TYPE);

        AuthLoginRespVO response = new AuthLoginRespVO();
        response.setAccessToken(accessToken);
        response.setTenantId(tenantId);
        return response;
    }

    private SysUserDO requireUserByMobile(String mobile) {
        String normalizedMobile = trimToNull(mobile);
        if (!StringUtils.hasText(normalizedMobile)) {
            throw new ServiceException(ErrorCodeConstants.MEMBER_INVITE_NOT_FOUND);
        }

        SysUserDO user = userMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getMobile, normalizedMobile));
        if (user == null) {
            throw new ServiceException(ErrorCodeConstants.MEMBER_INVITE_NOT_FOUND);
        }
        return user;
    }

    private SysTenantUserDO requirePendingInvite(Long userId, Long tenantId) {
        SysTenantUserDO tenantUser = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getTenantId, tenantId)
                .eq(SysTenantUserDO::getUserId, userId)
                .eq(SysTenantUserDO::getStatus, TenantUserStatus.NOT_JOINED));
        if (tenantUser == null) {
            throw new ServiceException(ErrorCodeConstants.MEMBER_INVITE_NOT_FOUND);
        }
        return tenantUser;
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.trim().length() < MIN_PASSWORD_LENGTH) {
            throw new ServiceException(ErrorCodeConstants.MEMBER_INVITE_PASSWORD_WEAK);
        }
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
