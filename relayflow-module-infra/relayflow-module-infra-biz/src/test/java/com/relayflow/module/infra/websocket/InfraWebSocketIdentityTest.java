package com.relayflow.module.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.websocket.core.*;
import com.relayflow.framework.websocket.security.WebSocketSessionGuard;
import com.relayflow.module.infra.websocket.router.DomainMessageRouter;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.*;

class InfraWebSocketIdentityTest {
    @Test
    void invalidSessionDoesNotRouteBusinessMessagesOrPingAndDoesNotRefreshOnlineState() throws Exception {
        DomainMessageRouter router = mock(DomainMessageRouter.class);
        WebSocketOnlineService online = mock(WebSocketOnlineService.class);
        WebSocketSessionGuard guard = mock(WebSocketSessionGuard.class);
        WebSocketSession session = mock(WebSocketSession.class);
        InfraWebSocketHandler handler = new InfraWebSocketHandler(new ObjectMapper(),
                new WebSocketSessionRegistry(), online, mock(WebSocketInstanceIdProvider.class), router, guard);
        handler.handleMessage(session, new TextMessage("{\"domain\":\"im\",\"type\":\"message.send\"}"));
        handler.handleMessage(session, new TextMessage("{\"domain\":\"system\",\"type\":\"ping\"}"));
        verify(guard, times(2)).isValid(session);
        verifyNoInteractions(router, online);
    }
}
