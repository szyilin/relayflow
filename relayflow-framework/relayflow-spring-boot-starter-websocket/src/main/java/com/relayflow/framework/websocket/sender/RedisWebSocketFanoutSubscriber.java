package com.relayflow.framework.websocket.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.websocket.config.WebSocketProperties;
import com.relayflow.framework.websocket.core.WebSocketInstanceIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "relayflow.websocket", name = "sender-type", havingValue = "redis")
@RequiredArgsConstructor
public class RedisWebSocketFanoutSubscriber implements MessageListener {

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ObjectMapper objectMapper;
    private final LocalWebSocketMessageSender localSender;
    private final WebSocketInstanceIdProvider instanceIdProvider;
    private final WebSocketProperties webSocketProperties;

    @PostConstruct
    public void subscribe() {
        if (!webSocketProperties.isEnable()) {
            return;
        }
        redisMessageListenerContainer.addMessageListener(this, new PatternTopic("t:*:ws:fanout"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            RedisWebSocketMessageSender.WebSocketFanoutMessage fanout =
                    objectMapper.readValue(body, RedisWebSocketMessageSender.WebSocketFanoutMessage.class);
            if (instanceIdProvider.getInstanceId().equals(fanout.getSourceInstanceId())) {
                return;
            }
            if (fanout.getTenantId() == null || fanout.getUserIds() == null || fanout.getUserIds().isEmpty()) {
                return;
            }
            localSender.send(fanout.getTenantId(), fanout.getUserIds(), fanout.getEnvelope());
        } catch (Exception ex) {
            log.debug("Ignore invalid WebSocket fanout message: {}", ex.getMessage());
        }
    }
}
