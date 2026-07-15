package com.relayflow.module.system.service.tenant;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterTenantSummaryVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private static final String ADMIN_USER_TYPE = "admin";

    private final SysTenantMapper tenantMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysUserMapper userMapper;
    private final TenantProperties tenantProperties;
    private final JwtTokenService jwtTokenService;

    @Override
    public SysTenantDO getTenant(Long tenantId) {
        SysTenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException(ErrorCodeConstants.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    @Override
    public SysTenantDO getDefaultTenant() {
        return getTenant(tenantProperties.getDefaultId());
    }

    @Override
    public void assertDeletable(Long tenantId) {
        if (tenantId != null && tenantId == tenantProperties.getDefaultId()) {
            throw new ServiceException(ErrorCodeConstants.TENANT_DEFAULT_DELETE_FORBIDDEN);
        }
    }

    @Override
    public List<AuthRegisterTenantSummaryVO> listMyTenants() {
        return listMyTenants(SecurityFrameworkUtils.requireLoginUserId());
    }

    @Override
    public List<AuthRegisterTenantSummaryVO> listMyTenants(Long userId) {
        List<SysTenantUserDO> memberships = loadActiveMemberships(userId);
        return memberships.stream()
                .map(membership -> toSummary(membership, userId))
                .toList();
    }

    @Override
    public AuthLoginRespVO switchMyTenant(Long tenantId) {
        return switchTenant(SecurityFrameworkUtils.requireLoginUserId(), tenantId);
    }

    @Override
    public AuthLoginRespVO switchTenant(Long userId, Long tenantId) {
        requireActiveMembership(userId, tenantId);

        SysUserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }

        TenantContextHolder.set(tenantId);
        String accessToken = jwtTokenService.createAccessToken(
                user.getId(), user.getUsername(), tenantId, ADMIN_USER_TYPE);

        AuthLoginRespVO response = new AuthLoginRespVO();
        response.setAccessToken(accessToken);
        response.setTenantId(tenantId);
        return response;
    }

    private List<SysTenantUserDO> loadActiveMemberships(Long userId) {
        return tenantUserMapper.selectList(Wrappers.<SysTenantUserDO>lambdaQuery()
                        .eq(SysTenantUserDO::getUserId, userId)
                        .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE))
                .stream()
                .sorted(Comparator.comparing(SysTenantUserDO::getTenantId))
                .toList();
    }

    private void requireActiveMembership(Long userId, Long tenantId) {
        SysTenantUserDO membership = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getUserId, userId)
                .eq(SysTenantUserDO::getTenantId, tenantId)
                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE));
        if (membership == null) {
            throw new ServiceException(ErrorCodeConstants.TENANT_SWITCH_FORBIDDEN);
        }
    }

    private AuthRegisterTenantSummaryVO toSummary(SysTenantUserDO membership, Long userId) {
        SysTenantDO tenant = getTenant(membership.getTenantId());
        AuthRegisterTenantSummaryVO summary = new AuthRegisterTenantSummaryVO();
        summary.setTenantId(tenant.getId());
        summary.setTenantName(tenant.getName());
        summary.setOwner(Objects.equals(userId, tenant.getOwnerUserId()));
        return summary;
    }
}
