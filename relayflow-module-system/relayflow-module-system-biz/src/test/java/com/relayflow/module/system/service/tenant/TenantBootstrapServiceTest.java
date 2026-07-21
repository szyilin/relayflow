package com.relayflow.module.system.service.tenant;

import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.module.system.dal.dataobject.SysPermissionDO;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;
import com.relayflow.module.system.dal.dataobject.SysRolePermissionDO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysUserRoleDO;
import com.relayflow.module.system.dal.mapper.SysPermissionMapper;
import com.relayflow.module.system.dal.mapper.SysRoleMapper;
import com.relayflow.module.system.dal.mapper.SysRolePermissionMapper;
import com.relayflow.module.system.dal.mapper.SysTenantMapper;
import com.relayflow.module.system.dal.mapper.SysUserDeptMapper;
import com.relayflow.module.system.dal.mapper.SysUserRoleMapper;
import com.relayflow.module.system.enums.DataScope;
import com.relayflow.module.system.enums.RoleType;
import com.relayflow.module.system.service.dept.DeptService;
import com.relayflow.module.system.service.permission.PermissionCacheEvictor;
import com.relayflow.module.system.service.permission.PermissionService;
import com.relayflow.module.system.service.permission.PermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantBootstrapServiceTest {

    private static final long TENANT_ID = 2L;
    private static final long OWNER_USER_ID = 100L;
    private static final long ROOT_DEPT_ID = 20L;

    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private SysPermissionMapper permissionMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysRolePermissionMapper rolePermissionMapper;
    @Mock
    private SysUserDeptMapper userDeptMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private DeptService deptService;
    @Mock
    private PermissionCacheEvictor permissionCacheEvictor;
    @Spy
    private TenantProperties tenantProperties = new TenantProperties();

    @InjectMocks
    private TenantBootstrapServiceImpl tenantBootstrapService;

    private final AtomicLong nextId = new AtomicLong(9000L);
    private final List<SysPermissionDO> copiedPermissions = new ArrayList<>();
    private final List<SysRolePermissionDO> rolePermissions = new ArrayList<>();
    private SysRoleDO superAdminRole;

    @BeforeEach
    void setUp() {
        tenantProperties.setDefaultId(1L);

        SysTenantDO tenant = new SysTenantDO();
        tenant.setId(TENANT_ID);
        tenant.setName("测试企业");
        when(tenantMapper.selectById(TENANT_ID)).thenReturn(tenant);
        when(deptService.getOrCreateRootDept(TENANT_ID)).thenReturn(ROOT_DEPT_ID);

        SysPermissionDO templatePermission = new SysPermissionDO();
        templatePermission.setId(1101L);
        templatePermission.setTenantId(1L);
        templatePermission.setParentId(0L);
        templatePermission.setName("用户查询");
        templatePermission.setCode("system:user:query");
        templatePermission.setType(2);
        templatePermission.setSort(1);
        templatePermission.setStatus(0);
        when(permissionMapper.selectList(any())).thenReturn(List.of(templatePermission));

        doAnswer(invocation -> {
            SysPermissionDO permission = invocation.getArgument(0);
            permission.setId(nextId.incrementAndGet());
            copiedPermissions.add(permission);
            return 1;
        }).when(permissionMapper).insert(any(SysPermissionDO.class));

        doAnswer(invocation -> {
            SysRoleDO role = invocation.getArgument(0);
            role.setId(nextId.incrementAndGet());
            superAdminRole = role;
            return 1;
        }).when(roleMapper).insert(any(SysRoleDO.class));

        doAnswer(invocation -> {
            SysRolePermissionDO rolePermission = invocation.getArgument(0);
            rolePermissions.add(rolePermission);
            return 1;
        }).when(rolePermissionMapper).insert(any(SysRolePermissionDO.class));
    }

    @Test
    void bootstrapOwner_grantsAdminPermissions() {
        tenantBootstrapService.bootstrapOwner(TENANT_ID, OWNER_USER_ID);

        verify(tenantMapper).updateById(org.mockito.ArgumentMatchers.<SysTenantDO>argThat(tenant ->
                TENANT_ID == tenant.getId() && OWNER_USER_ID == tenant.getOwnerUserId()));
        verify(deptService).getOrCreateRootDept(TENANT_ID);
        verify(userDeptMapper).insert(org.mockito.ArgumentMatchers.<com.relayflow.module.system.dal.dataobject.SysUserDeptDO>argThat(relation ->
                TENANT_ID == relation.getTenantId()
                        && OWNER_USER_ID == relation.getUserId()
                        && ROOT_DEPT_ID == relation.getDeptId()
                        && Integer.valueOf(1).equals(relation.getPrimaryFlag())));

        ArgumentCaptor<SysUserRoleDO> userRoleCaptor = ArgumentCaptor.forClass(SysUserRoleDO.class);
        verify(userRoleMapper).insert(userRoleCaptor.capture());
        SysUserRoleDO ownerRole = userRoleCaptor.getValue();
        assertTrue(superAdminRole != null);
        assertTrue("super_admin".equals(superAdminRole.getCode()));
        assertTrue(RoleType.SYSTEM == superAdminRole.getRoleType());
        assertTrue(DataScope.ALL == superAdminRole.getDataScope());
        assertTrue(!copiedPermissions.isEmpty());
        assertTrue(rolePermissions.size() == copiedPermissions.size());

        PermissionService permissionService = buildPermissionService();
        assertTrue(permissionService.isAdmin(OWNER_USER_ID, TENANT_ID));
    }

    private PermissionService buildPermissionService() {
        when(userRoleMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(ownerRoleBinding()));
        when(roleMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of(superAdminRole));
        when(rolePermissionMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(rolePermissions);

        when(permissionMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(copiedPermissions);

        com.relayflow.module.system.service.permission.PermissionCacheService permissionCacheService =
                org.mockito.Mockito.mock(com.relayflow.module.system.service.permission.PermissionCacheService.class);
        org.mockito.Mockito.doAnswer(invocation -> {
            java.util.function.Supplier<?> supplier = invocation.getArgument(2);
            return supplier.get();
        }).when(permissionCacheService).getOrLoad(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());

        return new PermissionServiceImpl(
                org.mockito.Mockito.mock(com.relayflow.module.system.dal.mapper.SysUserMapper.class),
                org.mockito.Mockito.mock(com.relayflow.module.system.dal.mapper.SysTenantUserMapper.class),
                userRoleMapper,
                roleMapper,
                rolePermissionMapper,
                permissionMapper,
                tenantProperties,
                permissionCacheService);
    }

    private SysUserRoleDO ownerRoleBinding() {
        SysUserRoleDO relation = new SysUserRoleDO();
        relation.setTenantId(TENANT_ID);
        relation.setUserId(OWNER_USER_ID);
        relation.setRoleId(superAdminRole.getId());
        return relation;
    }
}
