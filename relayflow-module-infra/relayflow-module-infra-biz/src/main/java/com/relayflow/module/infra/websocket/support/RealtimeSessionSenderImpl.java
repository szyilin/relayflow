package com.relayflow.module.infra.websocket.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.websocket.core.WebSocketSessionRegistry;
import com.relayflow.framework.websocket.sender.LocalWebSocketMessageSender;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeSessionSenderImpl implements RealtimeSessionSender {

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void send(RealtimeSessionContext session, RealtimeEnvelopeDTO envelope) {
        WebSocketSession webSocketSession = sessionRegistry.getSession(session.sessionId());
        if (webSocketSession == null || !webSocketSession.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(RealtimeEnvelopeConverter.toFramework(envelope));
            LocalWebSocketMessageSender.writeSafely(webSocketSession, new TextMessage(json));
        } catch (Exception ex) {
            log.warn("Realtime session send failed (best-effort): sessionId={}", session.sessionId(), ex);
        }
    }
}
