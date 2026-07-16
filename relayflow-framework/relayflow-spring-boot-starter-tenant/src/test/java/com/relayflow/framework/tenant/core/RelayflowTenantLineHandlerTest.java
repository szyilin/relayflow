package com.relayflow.framework.tenant.core;

import com.relayflow.framework.tenant.config.TenantProperties;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelayflowTenantLineHandlerTest {

    private final RelayflowTenantLineHandler handler = new RelayflowTenantLineHandler(new TenantProperties());

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void usesDefaultTenantWhenContextMissing() {
        LongValue tenantId = (LongValue) handler.getTenantId();
        assertEquals(1L, tenantId.getValue());
    }

    @Test
    void usesContextTenantWhenPresent() {
        TenantContextHolder.set(2L);
        LongValue tenantId = (LongValue) handler.getTenantId();
        assertEquals(2L, tenantId.getValue());
    }

    @Test
    void ignoresGlobalTenantMetadataTables() {
        assertTrue(handler.ignoreTable("sys_tenant"));
        assertTrue(handler.ignoreTable("sys_tenant_user"));
        assertTrue(handler.ignoreTable("sys_user"));
        assertTrue(handler.ignoreTable("im_bot"));
    }

    @Test
    void doesNotIgnoreBusinessTables() {
        assertFalse(handler.ignoreTable("sys_dept"));
        assertFalse(handler.ignoreTable("infra_file"));
        assertFalse(handler.ignoreTable("im_message"));
    }
}
