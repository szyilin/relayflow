package com.relayflow.module.infra.framework.realtime;

import com.relayflow.framework.websocket.config.WebSocketProperties;
import com.relayflow.framework.websocket.security.JwtWebSocketHandshakeInterceptor;
import com.relayflow.module.infra.api.realtime.RealtimeDomainMessageHandler;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.websocket.InfraWebSocketHandler;
import com.relayflow.module.infra.websocket.router.DomainMessageRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@ConditionalOnProperty(prefix = "relayflow.websocket", name = "enable", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RealtimeWebSocketConfiguration implements WebSocketConfigurer {

    private final WebSocketProperties webSocketProperties;
    private final InfraWebSocketHandler infraWebSocketHandler;
    private final JwtWebSocketHandshakeInterceptor handshakeInterceptor;

    @Bean
    public DomainMessageRouter domainMessageRouter(java.util.List<RealtimeDomainMessageHandler> handlers,
                                                   RealtimeSessionSender sessionSender) {
        return new DomainMessageRouter(handlers, sessionSender);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(infraWebSocketHandler, webSocketProperties.getPath())
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(webSocketProperties.allowedOriginArray());
    }
}
