package com.relayflow.framework.websocket.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.redis.core.TenantRedisKeyBuilder;
import com.relayflow.framework.websocket.core.RealtimeEnvelope;
import com.relayflow.framework.websocket.core.WebSocketMessageSender;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collection;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class RedisWebSocketMessageSender implements WebSocketMessageSender {

    private static final String FANOUT_NAMESPACE = "ws";
    private static final String FANOUT_SUFFIX = "fanout";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;
    private final LocalWebSocketMessageSender localSender;

    @Override
    public void send(Long tenantId, Collection<Long> userIds, RealtimeEnvelope envelope) {
        localSender.send(tenantId, userIds, envelope);
        WebSocketFanoutMessage fanout = new WebSocketFanoutMessage();
        fanout.setTenantId(tenantId);
        fanout.setUserIds(List.copyOf(userIds));
        fanout.setEnvelope(envelope);
        fanout.setSourceInstanceId(instanceId);
        try {
            String json = objectMapper.writeValueAsString(fanout);
            stringRedisTemplate.convertAndSend(fanoutChannel(tenantId), json);
        } catch (Exception ex) {
            log.warn("Redis WebSocket fanout publish failed: {}", ex.getMessage());
        }
    }

    static String fanoutChannel(Long tenantId) {
        return TenantRedisKeyBuilder.build(tenantId, FANOUT_NAMESPACE, FANOUT_SUFFIX);
    }

    @Data
    public static class WebSocketFanoutMessage {
        private Long tenantId;
        private Collection<Long> userIds;
        private RealtimeEnvelope envelope;
        private String sourceInstanceId;
    }
}
