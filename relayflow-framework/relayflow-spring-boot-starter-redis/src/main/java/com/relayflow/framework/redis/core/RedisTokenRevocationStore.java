package com.relayflow.framework.redis.core;

import com.relayflow.framework.redis.config.CacheProperties;
import com.relayflow.framework.security.core.TokenRevocationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.Duration;

@RequiredArgsConstructor
public class RedisTokenRevocationStore implements TokenRevocationStore {

    private static final String NAMESPACE = "auth:blacklist";

    private final RelayflowRedisCache redisCache;
    private final CacheProperties cacheProperties;

    @Override
    public void revoke(String jti, Long tenantId, Duration ttl) {
        if (!cacheProperties.isEnabled() || !StringUtils.hasText(jti) || tenantId == null || ttl == null || ttl.isZero()) {
            return;
        }
        String key = TenantRedisKeyBuilder.build(tenantId, NAMESPACE, jti);
        redisCache.put(key, "1", ttl);
    }

    @Override
    public boolean isRevoked(String jti, Long tenantId) {
        if (!cacheProperties.isEnabled() || !StringUtils.hasText(jti) || tenantId == null) {
            return false;
        }
        String key = TenantRedisKeyBuilder.build(tenantId, NAMESPACE, jti);
        return redisCache.hasKey(key);
    }
}
