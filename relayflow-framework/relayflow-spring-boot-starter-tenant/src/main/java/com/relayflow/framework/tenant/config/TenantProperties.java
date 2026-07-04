package com.relayflow.framework.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "relayflow.tenant")
public class TenantProperties {

    /**
     * When false, all requests bind to {@link #defaultId} without SaaS tenant UI.
     */
    private boolean enabled = false;

    /**
     * Default tenant id for single-tenant mode (V1 seed tenant).
     */
    private long defaultId = 1L;

    /**
     * HTTP header used when {@link #enabled} is true.
     */
    private String headerName = "X-Tenant-Id";
}
