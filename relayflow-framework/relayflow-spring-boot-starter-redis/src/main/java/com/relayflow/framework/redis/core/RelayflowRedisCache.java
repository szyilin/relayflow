package com.relayflow.framework.redis.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.redis.config.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 带经典问题防护的 Redis 缓存工具：
 * <ul>
 *   <li>穿透：缓存空值标记，短 TTL</li>
 *   <li>击穿：互斥锁 + double-check</li>
 *   <li>雪崩：TTL 随机抖动</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class RelayflowRedisCache {

    static final String NULL_MARKER = "__RF_NULL__";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    public <T> T getOrLoad(String key, TypeReference<T> typeReference, Duration ttl, Supplier<T> loader) {
        if (!properties.isEnabled()) {
            return loader.get();
        }

        T cached = readValue(key, typeReference);
        if (cached != null || isNullMarker(key)) {
            return cached;
        }

        String lockKey = key + ":lock";
        long deadline = System.currentTimeMillis() + properties.getLockWaitMillis();
        while (System.currentTimeMillis() < deadline) {
            if (tryLock(lockKey)) {
                try {
                    cached = readValue(key, typeReference);
                    if (cached != null || isNullMarker(key)) {
                        return cached;
                    }
                    T loaded = loader.get();
                    writeValue(key, loaded, ttl);
                    return loaded;
                } finally {
                    unlock(lockKey);
                }
            }

            cached = readValue(key, typeReference);
            if (cached != null || isNullMarker(key)) {
                return cached;
            }
            sleepBriefly();
        }

        log.warn("Cache lock wait timeout for key={}, loading directly", key);
        return loader.get();
    }

    public void evict(String key) {
        if (!properties.isEnabled()) {
            return;
        }
        stringRedisTemplate.delete(key);
    }

    public void put(String key, String value, Duration ttl) {
        if (!properties.isEnabled()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key, value, effectiveTtl(ttl));
    }

    public boolean hasKey(String key) {
        if (!properties.isEnabled()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private void writeValue(String key, Object value, Duration ttl) {
        if (value == null) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    NULL_MARKER,
                    Duration.ofSeconds(properties.getNullValueTtlSeconds()));
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), effectiveTtl(ttl));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize cache value for key=" + key, exception);
        }
    }

    private <T> T readValue(String key, TypeReference<T> typeReference) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (raw == null) {
            return null;
        }
        if (NULL_MARKER.equals(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, typeReference);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to deserialize cache key={}, evicting", key, exception);
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    private boolean isNullMarker(String key) {
        return NULL_MARKER.equals(stringRedisTemplate.opsForValue().get(key));
    }

    private Duration effectiveTtl(Duration ttl) {
        long baseSeconds = ttl != null ? ttl.getSeconds() : properties.getDefaultTtlSeconds();
        long jitter = properties.getTtlJitterSeconds() > 0
                ? ThreadLocalRandom.current().nextLong(properties.getTtlJitterSeconds() + 1)
                : 0L;
        return Duration.ofSeconds(baseSeconds + jitter);
    }

    private boolean tryLock(String lockKey) {
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                "1",
                Duration.ofMillis(properties.getLockLeaseMillis()));
        return Boolean.TRUE.equals(acquired);
    }

    private void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
