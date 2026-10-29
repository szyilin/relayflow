package com.relayflow.framework.redis.core;

import com.relayflow.framework.security.core.TokenRevocationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;

/** Security state must remain authoritative even when the optional business cache is disabled. */
@RequiredArgsConstructor
public class RedisTokenRevocationStore implements TokenRevocationStore {

    private final StringRedisTemplate redis;

    @Override
    public void revoke(String jti, Long tenantId, Duration ttl) {
        requireIdentity(jti, tenantId);
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redis.opsForValue().set(TenantRedisKeyBuilder.build(tenantId, "auth:blacklist", jti), "1", ttl);
    }

    @Override
    public boolean isRevoked(String jti, Long tenantId) {
        requireIdentity(jti, tenantId);
        Boolean revoked = redis.hasKey(TenantRedisKeyBuilder.build(tenantId, "auth:blacklist", jti));
        if (revoked == null) {
            throw new IllegalStateException("Revocation result unavailable");
        }
        return revoked;
    }

    private void requireIdentity(String jti, Long tenantId) {
        if (!StringUtils.hasText(jti) || tenantId == null) {
            throw new IllegalArgumentException("Revocation identity required");
        }
    }
}
