package com.relayflow.module.system.service.tenant;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.module.system.dal.mysql.SysTenantMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    private SysTenantMapper tenantMapper;

    @Spy
    private TenantProperties tenantProperties = new TenantProperties();

    @InjectMocks
    private TenantServiceImpl tenantService;

    @Test
    void rejectsDeletingDefaultTenant() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> tenantService.assertDeletable(1L));
        assertEquals(ErrorCodeConstants.TENANT_DEFAULT_DELETE_FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void allowsDeletingNonDefaultTenant() {
        assertDoesNotThrow(() -> tenantService.assertDeletable(2L));
    }
}
