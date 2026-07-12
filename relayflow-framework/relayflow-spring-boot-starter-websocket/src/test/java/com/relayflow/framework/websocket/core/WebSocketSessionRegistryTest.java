package com.relayflow.framework.websocket.core;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketSessionRegistryTest {

    @Test
    void isolatesSessionsByTenant() {
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();

        WebSocketSession tenantOneSession = mock(WebSocketSession.class);
        when(tenantOneSession.getId()).thenReturn("s1");
        when(tenantOneSession.isOpen()).thenReturn(true);

        WebSocketSession tenantTwoSession = mock(WebSocketSession.class);
        when(tenantTwoSession.getId()).thenReturn("s2");
        when(tenantTwoSession.isOpen()).thenReturn(true);

        registry.register(tenantOneSession, 1L, 100L);
        registry.register(tenantTwoSession, 2L, 100L);

        Collection<WebSocketSession> tenantOne = registry.getSessions(1L, 100L);
        Collection<WebSocketSession> tenantTwo = registry.getSessions(2L, 100L);

        assertEquals(1, tenantOne.size());
        assertEquals(1, tenantTwo.size());
        assertTrue(registry.hasSessions(1L, 100L));
        assertTrue(registry.hasSessions(2L, 100L));
        assertFalse(registry.hasSessions(1L, 200L));
    }
}
