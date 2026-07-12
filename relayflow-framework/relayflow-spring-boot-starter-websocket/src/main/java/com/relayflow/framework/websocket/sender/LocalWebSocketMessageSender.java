package com.relayflow.framework.websocket.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.websocket.core.RealtimeEnvelope;
import com.relayflow.framework.websocket.core.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class LocalWebSocketMessageSender {

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public void send(Long tenantId, Collection<Long> userIds, RealtimeEnvelope envelope) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize WebSocket envelope: {}", ex.getMessage());
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (Long userId : userIds) {
            for (WebSocketSession session : sessionRegistry.getSessions(tenantId, userId)) {
                writeSafely(session, message);
            }
        }
    }

    public static void writeSafely(WebSocketSession session, TextMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException ex) {
            log.debug("WebSocket send failed for session {}: {}", session.getId(), ex.getMessage());
        }
    }
}
