package com.relayflow.module.system.service.auth;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterTenantSummaryVO;
import com.relayflow.module.system.controller.app.vo.TenantSelectionDataVO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mysql.SysTenantUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.tenant.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String PASSWORD = "pass1234";
    private static final String ENCODED_PASSWORD = "$2a$encoded";

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysTenantUserMapper tenantUserMapper;
    @Mock
    private TenantService tenantService;
    @Spy
    private TenantProperties tenantProperties = new TenantProperties();
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private ObjectProvider<com.relayflow.framework.security.core.TokenRevocationStore> tokenRevocationStore;

    @InjectMocks
    private AuthServiceImpl authService;

    private SysUserDO user;

    @BeforeEach
    void setUp() {
        user = new SysUserDO();
        user.setId(100L);
        user.setUsername("13900001234");
        user.setMobile("13900001234");
        user.setPassword(ENCODED_PASSWORD);
    }

    @Test
    void loginWhenTenantDisabledUsesFirstActiveMembership() {
        tenantProperties.setEnabled(false);

        SysTenantUserDO membership = activeMembership(1L);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(tenantUserMapper.selectOne(any())).thenReturn(membership);
        when(jwtTokenService.createAccessToken(100L, "13900001234", 1L, "admin")).thenReturn("token-1");

        AuthLoginReqVO request = new AuthLoginReqVO();
        request.setUsername("13900001234");
        request.setPassword(PASSWORD);

        AuthLoginRespVO response = authService.login(request);

        assertEquals("token-1", response.getAccessToken());
        assertEquals(1L, response.getTenantId());
    }

    @Test
    void loginWhenMultiTenantWithoutTenantIdRequiresSelection() {
        tenantProperties.setEnabled(true);

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(tenantUserMapper.selectList(any())).thenReturn(List.of(activeMembership(1L), activeMembership(2L)));

        AuthRegisterTenantSummaryVO tenant1 = summary(1L, "企业 A");
        AuthRegisterTenantSummaryVO tenant2 = summary(2L, "企业 B");
        when(tenantService.listMyTenants(100L)).thenReturn(List.of(tenant1, tenant2));

        AuthLoginReqVO request = new AuthLoginReqVO();
        request.setUsername("13900001234");
        request.setPassword(PASSWORD);

        ServiceException exception = assertThrows(ServiceException.class, () -> authService.login(request));

        assertEquals(ErrorCodeConstants.TENANT_SELECTION_REQUIRED.getCode(), exception.getCode());
        TenantSelectionDataVO data = (TenantSelectionDataVO) exception.getData();
        assertNotNull(data);
        assertEquals(2, data.getTenants().size());
    }

    @Test
    void loginWhenMultiTenantWithTenantIdIssuesToken() {
        tenantProperties.setEnabled(true);

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(tenantUserMapper.selectList(any())).thenReturn(List.of(activeMembership(1L), activeMembership(2L)));
        when(jwtTokenService.createAccessToken(100L, "13900001234", 2L, "admin")).thenReturn("token-2");

        AuthLoginReqVO request = new AuthLoginReqVO();
        request.setUsername("13900001234");
        request.setPassword(PASSWORD);
        request.setTenantId(2L);

        AuthLoginRespVO response = authService.login(request);

        assertEquals("token-2", response.getAccessToken());
        assertEquals(2L, response.getTenantId());
    }

    @Test
    void loginByMobileWhenTenantEnabled() {
        tenantProperties.setEnabled(true);

        when(userMapper.selectOne(any()))
                .thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(tenantUserMapper.selectList(any())).thenReturn(List.of(activeMembership(3L)));
        when(jwtTokenService.createAccessToken(100L, "13900001234", 3L, "admin")).thenReturn("token-3");

        AuthLoginReqVO request = new AuthLoginReqVO();
        request.setUsername("13900001234");
        request.setPassword(PASSWORD);

        AuthLoginRespVO response = authService.login(request);

        assertEquals(3L, response.getTenantId());
    }

    @Test
    void loginWithSpacedMobileWhenTenantDisabled() {
        tenantProperties.setEnabled(false);

        SysTenantUserDO membership = activeMembership(1L);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(tenantUserMapper.selectOne(any())).thenReturn(membership);
        when(jwtTokenService.createAccessToken(100L, "13900001234", 1L, "admin")).thenReturn("token-spaced");

        AuthLoginReqVO request = new AuthLoginReqVO();
        request.setUsername("139 0000 1234");
        request.setPassword(PASSWORD);

        AuthLoginRespVO response = authService.login(request);

        assertEquals("token-spaced", response.getAccessToken());
    }

    private SysTenantUserDO activeMembership(long tenantId) {
        SysTenantUserDO membership = new SysTenantUserDO();
        membership.setUserId(100L);
        membership.setTenantId(tenantId);
        membership.setStatus(TenantUserStatus.ACTIVE);
        return membership;
    }

    private AuthRegisterTenantSummaryVO summary(long tenantId, String name) {
        AuthRegisterTenantSummaryVO summary = new AuthRegisterTenantSummaryVO();
        summary.setTenantId(tenantId);
        summary.setTenantName(name);
        summary.setOwner(false);
        return summary;
    }
}
