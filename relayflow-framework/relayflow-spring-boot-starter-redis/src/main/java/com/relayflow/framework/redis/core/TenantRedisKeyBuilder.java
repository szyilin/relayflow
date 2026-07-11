package com.relayflow.framework.redis.core;

import org.springframework.util.Assert;

/**
 * 租户维度 Redis key 构建器，格式 {@code t:{tenantId}:{namespace}:{suffix}}。
 */
public final class TenantRedisKeyBuilder {

    private TenantRedisKeyBuilder() {
    }

    public static String build(Long tenantId, String namespace, String suffix) {
        Assert.notNull(tenantId, "tenantId must not be null");
        Assert.hasText(namespace, "namespace must not be empty");
        Assert.hasText(suffix, "suffix must not be empty");
        return "t:" + tenantId + ":" + namespace + ":" + suffix;
    }
}
