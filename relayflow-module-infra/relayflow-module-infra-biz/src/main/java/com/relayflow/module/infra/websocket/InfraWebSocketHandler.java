package com.relayflow.module.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.websocket.core.WebSocketInstanceIdProvider;
import com.relayflow.framework.websocket.core.WebSocketOnlineService;
import com.relayflow.framework.websocket.core.WebSocketSessionAttributes;
import com.relayflow.framework.websocket.core.WebSocketSessionRegistry;
import com.relayflow.framework.websocket.core.RealtimeEnvelope;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;
import com.relayflow.module.infra.websocket.router.DomainMessageRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class InfraWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionRegistry sessionRegistry;
    private final WebSocketOnlineService onlineService;
    private final WebSocketInstanceIdProvider instanceIdProvider;
    private final DomainMessageRouter domainMessageRouter;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long tenantId = (Long) session.getAttributes().get(WebSocketSessionAttributes.TENANT_ID);
        Long userId = (Long) session.getAttributes().get(WebSocketSessionAttributes.USER_ID);
        sessionRegistry.register(session, tenantId, userId);
        onlineService.markOnline(tenantId, userId, instanceIdProvider.getInstanceId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long tenantId = (Long) session.getAttributes().get(WebSocketSessionAttributes.TENANT_ID);
        Long userId = (Long) session.getAttributes().get(WebSocketSessionAttributes.USER_ID);
        sessionRegistry.unregister(session.getId());
        if (tenantId != null && userId != null) {
            onlineService.markOfflineIfNoLocalSessions(tenantId, userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long tenantId = (Long) session.getAttributes().get(WebSocketSessionAttributes.TENANT_ID);
        Long userId = (Long) session.getAttributes().get(WebSocketSessionAttributes.USER_ID);
        if (tenantId == null || userId == null) {
            return;
        }
        try {
            RealtimeEnvelope envelope = objectMapper.readValue(message.getPayload(), RealtimeEnvelope.class);
            if (envelope.getDomain() == null || envelope.getType() == null) {
                log.debug("Ignore WebSocket message missing domain/type from session {}", session.getId());
                return;
            }
            RealtimeSessionContext context = new RealtimeSessionContext(tenantId, userId, session.getId());
            domainMessageRouter.route(envelope, context);
        } catch (Exception ex) {
            log.debug("Failed to handle WebSocket message on session {}: {}", session.getId(), ex.getMessage());
        }
    }
}
