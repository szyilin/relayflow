package com.relayflow.framework.security.core;

/** Shared explicit exceptions: these requests never receive a bearer-derived identity. */
public final class AnonymousEndpoints {

    public static final String[] PATHS = {
            "/admin-api/system/auth/login", "/admin-api/system/auth/logout",
            "/admin-api/system/tenant/default", "/app-api/system/auth/register",
            "/app-api/infra/file/public/**", "/app-api/rtc/webhook", "/infra/ws"
    };

    private AnonymousEndpoints() {
    }
}
