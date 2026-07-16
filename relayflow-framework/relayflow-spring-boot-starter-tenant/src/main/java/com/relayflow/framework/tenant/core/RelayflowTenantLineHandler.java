package com.relayflow.framework.tenant.core;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.relayflow.framework.tenant.config.TenantProperties;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Set;

@RequiredArgsConstructor
public class RelayflowTenantLineHandler implements TenantLineHandler {

    private static final Set<String> IGNORED_TABLES = Set.of(
            "sys_tenant",
            "sys_tenant_user",
            "sys_user",
            // Platform-level bot catalog (no tenant_id column)
            "im_bot"
    );

    private final TenantProperties properties;

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContextHolder.get();
        if (tenantId == null) {
            tenantId = properties.getDefaultId();
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORED_TABLES.contains(tableName.toLowerCase());
    }
}
