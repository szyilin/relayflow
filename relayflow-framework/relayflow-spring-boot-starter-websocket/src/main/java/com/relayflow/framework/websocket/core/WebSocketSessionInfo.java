package com.relayflow.framework.websocket.core;

import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

@Getter
public class WebSocketSessionInfo {

    private final String sessionId;
    private final Long tenantId;
    private final Long userId;
    private final WebSocketSession session;

    public WebSocketSessionInfo(String sessionId, Long tenantId, Long userId, WebSocketSession session) {
        this.sessionId = sessionId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.session = session;
    }
}
