package com.relayflow.module.system.service.user;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.api.user.dto.UserInviteReqDTO;
import com.relayflow.module.infra.api.notify.NotifyInboxApi;
import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.enums.InfraNotifyType;
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
import com.relayflow.module.system.service.tenant.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private NotifyInboxApi notifyInboxApi;

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
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(deptService.getOrCreateRootDept(JWT_TENANT_ID)).thenReturn(100L);
        when(deptMapper.selectOne(any())).thenReturn(rootDept(JWT_TENANT_ID, 100L));
        when(userMapper.insert(org.mockito.ArgumentMatchers.<SysUserDO>any())).thenAnswer(invocation -> {
            invocation.getArgument(0, SysUserDO.class).setId(200L);
            return 1;
        });
        when(tenantService.getTenant(JWT_TENANT_ID)).thenReturn(tenant(JWT_TENANT_ID, "测试企业"));

        UserInviteReqDTO request = new UserInviteReqDTO();
        request.setMobile("13900009999");
        request.setNickname("受邀人");

        userService.inviteMember(request);

        ArgumentCaptor<SysTenantUserDO> captor = ArgumentCaptor.forClass(SysTenantUserDO.class);
        verify(tenantUserMapper).insert(captor.capture());
        assertEquals(JWT_TENANT_ID, captor.getValue().getTenantId());
        assertEquals(TenantUserStatus.NOT_JOINED, captor.getValue().getStatus());

        ArgumentCaptor<NotifyItemCommand> notifyCaptor = ArgumentCaptor.forClass(NotifyItemCommand.class);
        verify(notifyInboxApi).push(notifyCaptor.capture());
        assertEquals(InfraNotifyType.MEMBER_INVITE, notifyCaptor.getValue().getType());
        assertEquals(MOBILE, notifyCaptor.getValue().getMobile());
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
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(deptService.getOrCreateRootDept(1L)).thenReturn(100L);
        when(deptMapper.selectOne(any())).thenReturn(rootDept(1L, 100L));
        when(userMapper.insert(org.mockito.ArgumentMatchers.<SysUserDO>any())).thenAnswer(invocation -> {
            invocation.getArgument(0, SysUserDO.class).setId(201L);
            return 1;
        });
        when(tenantService.getTenant(1L)).thenReturn(tenant(1L, "默认企业"));

        UserInviteReqDTO request = new UserInviteReqDTO();
        request.setMobile("13900009997");

        userService.inviteMember(request);

        ArgumentCaptor<SysTenantUserDO> captor = ArgumentCaptor.forClass(SysTenantUserDO.class);
        verify(tenantUserMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getTenantId());
    }

    private SysDeptDO rootDept(long tenantId, long deptId) {
        SysDeptDO dept = new SysDeptDO();
        dept.setId(deptId);
        dept.setTenantId(tenantId);
        dept.setName("根部门");
        return dept;
    }

    private SysTenantDO tenant(long tenantId, String name) {
        SysTenantDO tenant = new SysTenantDO();
        tenant.setId(tenantId);
        tenant.setName(name);
        return tenant;
    }

    private static final String MOBILE = "13900009999";
}
