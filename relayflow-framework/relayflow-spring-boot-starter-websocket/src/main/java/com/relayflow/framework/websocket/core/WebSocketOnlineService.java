package com.relayflow.framework.websocket.core;

import com.relayflow.framework.redis.core.TenantRedisKeyBuilder;
import com.relayflow.framework.websocket.config.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WebSocketOnlineService {

    private static final String ONLINE_NAMESPACE = "online";

    private final StringRedisTemplate stringRedisTemplate;
    private final WebSocketProperties webSocketProperties;
    private final WebSocketSessionRegistry sessionRegistry;

    public void markOnline(Long tenantId, Long userId, String instanceId) {
        String key = onlineKey(tenantId, userId);
        Duration ttl = Duration.ofSeconds(webSocketProperties.getHeartbeatTtlSeconds());
        stringRedisTemplate.opsForValue().set(key, instanceId, ttl);
    }

    public void refreshOnline(Long tenantId, Long userId, String instanceId) {
        markOnline(tenantId, userId, instanceId);
    }

    public void markOfflineIfNoLocalSessions(Long tenantId, Long userId) {
        if (!sessionRegistry.hasSessions(tenantId, userId)) {
            stringRedisTemplate.delete(onlineKey(tenantId, userId));
        }
    }

    public boolean isOnline(Long tenantId, Long userId) {
        Boolean exists = stringRedisTemplate.hasKey(onlineKey(tenantId, userId));
        return Boolean.TRUE.equals(exists);
    }

    private static String onlineKey(Long tenantId, Long userId) {
        return TenantRedisKeyBuilder.build(tenantId, ONLINE_NAMESPACE, String.valueOf(userId));
    }
}
