package com.relayflow.framework.websocket.security;

import com.relayflow.framework.security.core.IdentityValidityService;
import com.relayflow.framework.security.core.SessionIdentity;
import com.relayflow.framework.websocket.core.WebSocketOnlineService;
import com.relayflow.framework.websocket.core.WebSocketSessionAttributes;
import com.relayflow.framework.websocket.core.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@RequiredArgsConstructor
public class WebSocketSessionGuard {

    private final IdentityValidityService identityValidityService;
    private final WebSocketSessionRegistry registry;
    private final WebSocketOnlineService onlineService;

    public boolean isValid(WebSocketSession session) {
        if (session == null || !session.isOpen()) {
            if (session != null) {
                disconnect(session);
            }
            return false;
        }
        try {
            Object value = session.getAttributes().get(WebSocketSessionAttributes.IDENTITY);
            identityValidityService.validate(value instanceof SessionIdentity identity ? identity : null);
            return true;
        } catch (RuntimeException ex) {
            disconnect(session);
            return false;
        }
    }

    public void disconnect(WebSocketSession session) {
        // Removal wins once, including racing senders, the scanner and close callbacks.
        if (!registry.unregister(session.getId())) {
            return;
        }
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (Exception ex) {
            log.debug("WebSocket close failed: sessionId={}", session.getId());
        } finally {
            cleanupOnline(session);
        }
    }

    public void cleanupOnline(WebSocketSession session) {
        Long tenantId = (Long) session.getAttributes().get(WebSocketSessionAttributes.TENANT_ID);
        Long userId = (Long) session.getAttributes().get(WebSocketSessionAttributes.USER_ID);
        if (tenantId != null && userId != null) {
            try {
                onlineService.markOfflineIfNoLocalSessions(tenantId, userId);
            } catch (RuntimeException ex) {
                // Identity fails closed even if Redis is down. The online key also has a TTL.
                log.debug("WebSocket online cleanup deferred: sessionId={}", session.getId());
            }
        }
    }

    @Scheduled(fixedDelayString = "${relayflow.websocket.identity-scan-interval-millis:30000}")
    public void inspectSessions() {
        for (WebSocketSession session : registry.getAllSessions()) {
            isValid(session);
        }
    }
}
