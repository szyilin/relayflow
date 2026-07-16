package com.relayflow.module.system.service.user;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import com.relayflow.module.system.api.user.dto.UserInviteReqDTO;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.mapper.SysDeptMapper;
import com.relayflow.module.system.dal.mapper.SysRoleMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserDeptMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserRoleMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.dept.DeptService;
import com.relayflow.module.system.service.permission.DataScopeHelper;
import com.relayflow.module.system.service.permission.PermissionCacheEvictor;
import com.relayflow.module.system.service.permission.PermissionService;
import com.relayflow.module.system.service.tenant.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplInviteTest {

    private static final long JWT_TENANT_ID = 42L;

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysTenantUserMapper tenantUserMapper;
    @Mock
    private SysUserDeptMapper userDeptMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysDeptMapper deptMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private DeptService deptService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Spy
    private TenantProperties tenantProperties = new TenantProperties();
    @Mock
    private DataScopeHelper dataScopeHelper;
    @Mock
    private PermissionCacheEvictor permissionCacheEvictor;
    @Mock
    private TenantService tenantService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private ImBotApi imBotApi;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        tenantProperties.setEnabled(true);
        tenantProperties.setDefaultId(1L);
        TenantContextHolder.set(JWT_TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void inviteMemberWhenTenantEnabledUsesJwtTenantId() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(tenantUserMapper.selectOne(any())).thenReturn(null);
        when(tenantUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(deptService.getOrCreateRootDept(JWT_TENANT_ID)).thenReturn(100L);
        when(deptMapper.selectOne(any())).thenReturn(rootDept(JWT_TENANT_ID, 100L));
        when(userMapper.insert(org.mockito.ArgumentMatchers.<SysUserDO>any())).thenAnswer(invocation -> {
            invocation.getArgument(0, SysUserDO.class).setId(200L);
            return 1;
        });
        UserInviteReqDTO request = new UserInviteReqDTO();
        request.setMobile("13900009999");
        request.setNickname("受邀人");

        userService.inviteMember(request);

        ArgumentCaptor<SysTenantUserDO> captor = ArgumentCaptor.forClass(SysTenantUserDO.class);
        verify(tenantUserMapper).insert(captor.capture());
        assertEquals(JWT_TENANT_ID, captor.getValue().getTenantId());
        assertEquals(TenantUserStatus.NOT_JOINED, captor.getValue().getStatus());
        verify(imBotApi, never()).send(any());
    }

    @Test
    void inviteMemberWithActiveMembershipSendsOrgAssistantBot() {
        SysUserDO existing = new SysUserDO();
        existing.setId(200L);
        existing.setMobile("13900009996");
        existing.setNickname("受邀人");
        when(userMapper.selectOne(any())).thenReturn(existing);
        when(tenantUserMapper.selectOne(any())).thenReturn(null);
        when(tenantUserMapper.selectCount(any())).thenReturn(1L);
        when(deptService.getOrCreateRootDept(JWT_TENANT_ID)).thenReturn(100L);
        when(deptMapper.selectOne(any())).thenReturn(rootDept(JWT_TENANT_ID, 100L));

        SysTenantDO tenant = new SysTenantDO();
        tenant.setId(JWT_TENANT_ID);
        tenant.setName("Acme");
        when(tenantService.getTenant(JWT_TENANT_ID)).thenReturn(tenant);

        LoginUser loginUser = new LoginUser(7L, "admin", JWT_TENANT_ID, "admin", List.of());
        SysUserDO inviter = new SysUserDO();
        inviter.setId(7L);
        inviter.setNickname("张三");
        when(userMapper.selectById(7L)).thenReturn(inviter);

        UserInviteReqDTO request = new UserInviteReqDTO();
        request.setMobile("13900009996");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            userService.inviteMember(request);
        }

        ArgumentCaptor<ImBotSendCommand> captor = ArgumentCaptor.forClass(ImBotSendCommand.class);
        verify(imBotApi).send(captor.capture());
        ImBotSendCommand command = captor.getValue();
        assertEquals("org-assistant", command.getBotCode());
        assertEquals("张三 邀请你加入 Acme", command.getText());
        assertEquals("MEMBER_INVITE:42", command.getDedupeKey());
        assertEquals("tenant", command.getEntityType());
        assertEquals("42", command.getEntityId());
        assertEquals(ImBotSendTarget.SCOPE_ALL_ACTIVE_MEMBERSHIPS, command.getTarget().getScope());
        assertEquals(200L, command.getTarget().getUserId());
        assertNull(command.getTarget().getTenantId());
    }

    @Test
    void inviteMemberWhenTenantEnabledWithoutContextFails() {
        TenantContextHolder.clear();

        UserInviteReqDTO request = new UserInviteReqDTO();
        request.setMobile("13900009998");

        ServiceException exception = assertThrows(ServiceException.class, () -> userService.inviteMember(request));
        assertEquals(ErrorCodeConstants.TENANT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void inviteMemberWhenTenantDisabledFallsBackToDefaultId() {
        tenantProperties.setEnabled(false);
        TenantContextHolder.clear();

        when(userMapper.selectOne(any())).thenReturn(null);
        when(tenantUserMapper.selectOne(any())).thenReturn(null);
        when(tenantUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(deptService.getOrCreateRootDept(1L)).thenReturn(100L);
        when(deptMapper.selectOne(any())).thenReturn(rootDept(1L, 100L));
        when(userMapper.insert(org.mockito.ArgumentMatchers.<SysUserDO>any())).thenAnswer(invocation -> {
            invocation.getArgument(0, SysUserDO.class).setId(201L);
            return 1;
        });
        UserInviteReqDTO request = new UserInviteReqDTO();
        request.setMobile("13900009997");

        userService.inviteMember(request);

        ArgumentCaptor<SysTenantUserDO> captor = ArgumentCaptor.forClass(SysTenantUserDO.class);
        verify(tenantUserMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getTenantId());
        verify(imBotApi, never()).send(any());
    }

    private SysDeptDO rootDept(long tenantId, long deptId) {
        SysDeptDO dept = new SysDeptDO();
        dept.setId(deptId);
        dept.setTenantId(tenantId);
        dept.setName("根部门");
        return dept;
    }
}
