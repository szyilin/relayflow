package com.relayflow.module.system.service.permission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.relayflow.framework.redis.config.CacheProperties;
import com.relayflow.framework.redis.core.RelayflowRedisCache;
import com.relayflow.framework.redis.core.TenantRedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private static final String NAMESPACE = "auth:perms";
    private static final TypeReference<LinkedHashSet<String>> PERMISSION_SET_TYPE = new TypeReference<>() {
    };

    private final RelayflowRedisCache redisCache;
    private final CacheProperties cacheProperties;

    public Set<String> getOrLoad(Long tenantId, Long userId, Supplier<Set<String>> loader) {
        String key = permissionKey(tenantId, userId);
        Duration ttl = Duration.ofSeconds(cacheProperties.getPermissionTtlSeconds());
        LinkedHashSet<String> cached = redisCache.getOrLoad(key, PERMISSION_SET_TYPE, ttl, () -> toLinkedHashSet(loader.get()));
        return cached != null ? cached : Set.of();
    }

    public void evictUser(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        redisCache.evict(permissionKey(tenantId, userId));
    }

    private String permissionKey(Long tenantId, Long userId) {
        return TenantRedisKeyBuilder.build(tenantId, NAMESPACE, String.valueOf(userId));
    }

    private LinkedHashSet<String> toLinkedHashSet(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return codes instanceof LinkedHashSet<String> linked ? linked : new LinkedHashSet<>(codes);
    }
}
