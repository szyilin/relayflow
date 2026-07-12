package com.relayflow.framework.websocket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.websocket.core.WebSocketInstanceIdProvider;
import com.relayflow.framework.websocket.core.WebSocketMessageSender;
import com.relayflow.framework.websocket.core.WebSocketOnlineService;
import com.relayflow.framework.websocket.core.WebSocketSessionRegistry;
import com.relayflow.framework.websocket.sender.LocalWebSocketMessageSender;
import com.relayflow.framework.websocket.sender.RedisWebSocketMessageSender;
import com.relayflow.framework.websocket.security.JwtWebSocketHandshakeInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@AutoConfiguration
@ConditionalOnProperty(prefix = "relayflow.websocket", name = "enable", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WebSocketProperties.class)
@Import({
        WebSocketSessionRegistry.class,
        WebSocketOnlineService.class,
        WebSocketInstanceIdProvider.class,
        JwtWebSocketHandshakeInterceptor.class,
        LocalWebSocketMessageSender.class
})
public class WebSocketAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "relayflow.websocket", name = "sender-type", havingValue = "local", matchIfMissing = true)
    public WebSocketMessageSender localWebSocketMessageSender(LocalWebSocketMessageSender localSender) {
        return localSender;
    }

    @Bean
    @ConditionalOnProperty(prefix = "relayflow.websocket", name = "sender-type", havingValue = "redis")
    public WebSocketMessageSender redisWebSocketMessageSender(StringRedisTemplate stringRedisTemplate,
                                                              ObjectMapper objectMapper,
                                                              WebSocketInstanceIdProvider instanceIdProvider,
                                                              LocalWebSocketMessageSender localSender) {
        return new RedisWebSocketMessageSender(
                stringRedisTemplate, objectMapper, instanceIdProvider.getInstanceId(), localSender);
    }

    @Bean
    @ConditionalOnProperty(prefix = "relayflow.websocket", name = "sender-type", havingValue = "redis")
    public RedisMessageListenerContainer redisMessageListenerContainer(StringRedisTemplate stringRedisTemplate) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(stringRedisTemplate.getConnectionFactory());
        return container;
    }
}
