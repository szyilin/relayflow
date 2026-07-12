package com.relayflow.module.system.service.auth;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.module.system.controller.app.vo.AuthRegisterReqVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterRespVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mysql.SysTenantMapper;
import com.relayflow.module.system.dal.mysql.SysTenantUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.tenant.TenantBootstrapService;
import com.relayflow.module.system.service.tenant.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRegisterServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private SysTenantUserMapper tenantUserMapper;
    @Mock
    private TenantService tenantService;
    @Mock
    private TenantBootstrapService tenantBootstrapService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Spy
    private TenantProperties tenantProperties = new TenantProperties();

    @InjectMocks
    private AuthRegisterServiceImpl authRegisterService;

    private AuthRegisterReqVO request;

    @BeforeEach
    void setUp() {
        tenantProperties.setEnabled(true);
        tenantProperties.setAllowOpenRegister(true);

        request = new AuthRegisterReqVO();
        request.setMobile("13900001234");
        request.setPassword("pass1234");
        request.setNickname("张三");
        request.setTenantName("张三的工作室");
    }

    @Test
    void rejectsWhenOpenRegisterDisabled() {
        tenantProperties.setEnabled(false);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authRegisterService.register(request));

        assertEquals(ErrorCodeConstants.AUTH_REGISTER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    void rejectsExistingActiveMobile() {
        SysUserDO existing = new SysUserDO();
        existing.setId(50L);
        existing.setMobile("13900001234");
        when(userMapper.selectOne(any())).thenReturn(existing);
        when(tenantUserMapper.selectCount(any())).thenReturn(1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authRegisterService.register(request));

        assertEquals(ErrorCodeConstants.USER_MOBILE_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void registerCreatesUserTenantAndBootstraps() {
        stubSuccessfulRegisterFlow();

        AuthRegisterRespVO response = authRegisterService.register(request);

        assertEquals("token-abc", response.getAccessToken());
        assertEquals(200001L, response.getTenantId());
        assertEquals(1, response.getTenants().size());
        assertTrue(response.getTenants().get(0).getOwner());

        ArgumentCaptor<SysUserDO> userCaptor = ArgumentCaptor.forClass(SysUserDO.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals("13900001234", userCaptor.getValue().getUsername());
        assertEquals("张三", userCaptor.getValue().getNickname());

        verify(tenantMapper).insert(any(SysTenantDO.class));
        verify(tenantUserMapper).insert(org.mockito.ArgumentMatchers.<SysTenantUserDO>argThat(relation ->
                Long.valueOf(200001L).equals(relation.getTenantId())
                        && Long.valueOf(100L).equals(relation.getUserId())
                        && TenantUserStatus.ACTIVE == relation.getStatus()));
        verify(tenantBootstrapService).bootstrapOwner(200001L, 100L);
    }

    @Test
    void registerActivatesPendingInvitesForExistingUser() {
        SysUserDO invited = new SysUserDO();
        invited.setId(100L);
        invited.setUsername("13900001234");
        invited.setMobile("13900001234");
        when(userMapper.selectOne(any())).thenReturn(invited);
        when(tenantUserMapper.selectCount(any())).thenReturn(0L, 0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            SysTenantDO tenant = invocation.getArgument(0);
            tenant.setId(200001L);
            return 1;
        }).when(tenantMapper).insert(any(SysTenantDO.class));
        when(jwtTokenService.createAccessToken(eq(100L), eq("13900001234"), eq(200001L), eq("admin")))
                .thenReturn("token-abc");

        SysTenantUserDO pending = new SysTenantUserDO();
        pending.setId(9L);
        pending.setTenantId(1L);
        pending.setUserId(100L);
        pending.setStatus(TenantUserStatus.NOT_JOINED);
        when(tenantUserMapper.selectList(any()))
                .thenReturn(List.of(pending), List.of(activeMembership(200001L), pendingWithActive(1L)));

        SysTenantDO createdTenant = new SysTenantDO();
        createdTenant.setId(200001L);
        createdTenant.setName("张三的工作室");
        createdTenant.setOwnerUserId(100L);
        SysTenantDO invitedTenant = new SysTenantDO();
        invitedTenant.setId(1L);
        invitedTenant.setName("默认企业");
        when(tenantService.getTenant(200001L)).thenReturn(createdTenant);
        when(tenantService.getTenant(1L)).thenReturn(invitedTenant);

        authRegisterService.register(request);

        verify(userMapper, never()).insert(any(SysUserDO.class));
        verify(userMapper).updateById(invited);
        verify(tenantUserMapper).updateById(org.mockito.ArgumentMatchers.<SysTenantUserDO>argThat(relation ->
                TenantUserStatus.ACTIVE == relation.getStatus() && Long.valueOf(1L).equals(relation.getTenantId())));
    }

    private void stubSuccessfulRegisterFlow() {
        when(userMapper.selectOne(any())).thenReturn(null);
        lenient().when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            SysTenantDO tenant = invocation.getArgument(0);
            tenant.setId(200001L);
            return 1;
        }).when(tenantMapper).insert(any(SysTenantDO.class));
        when(jwtTokenService.createAccessToken(eq(100L), eq("13900001234"), eq(200001L), eq("admin")))
                .thenReturn("token-abc");
        doAnswer(invocation -> {
            SysUserDO user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(userMapper).insert(any(SysUserDO.class));
        when(tenantUserMapper.selectList(any())).thenReturn(
                Collections.emptyList(),
                List.of(activeMembership(200001L)));

        SysTenantDO createdTenant = new SysTenantDO();
        createdTenant.setId(200001L);
        createdTenant.setName("张三的工作室");
        createdTenant.setOwnerUserId(100L);
        when(tenantService.getTenant(200001L)).thenReturn(createdTenant);
    }

    private SysTenantUserDO activeMembership(Long tenantId) {
        SysTenantUserDO membership = new SysTenantUserDO();
        membership.setTenantId(tenantId);
        membership.setUserId(100L);
        membership.setStatus(TenantUserStatus.ACTIVE);
        return membership;
    }

    private SysTenantUserDO pendingWithActive(Long tenantId) {
        SysTenantUserDO membership = new SysTenantUserDO();
        membership.setTenantId(tenantId);
        membership.setUserId(100L);
        membership.setStatus(TenantUserStatus.ACTIVE);
        return membership;
    }
}
