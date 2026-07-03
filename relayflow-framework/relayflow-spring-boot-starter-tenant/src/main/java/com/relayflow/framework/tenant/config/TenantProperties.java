package com.relayflow.framework.tenant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDefaultId() {
        return defaultId;
    }

    public void setDefaultId(long defaultId) {
        this.defaultId = defaultId;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
}
