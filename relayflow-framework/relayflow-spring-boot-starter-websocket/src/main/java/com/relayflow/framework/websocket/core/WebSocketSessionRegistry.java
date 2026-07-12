package com.relayflow.framework.websocket.core;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private final Map<String, WebSocketSessionInfo> sessionsById = new ConcurrentHashMap<>();
    private final Map<UserKey, Set<String>> sessionIdsByUser = new ConcurrentHashMap<>();

    public void register(WebSocketSession session, Long tenantId, Long userId) {
        WebSocketSessionInfo info = new WebSocketSessionInfo(session.getId(), tenantId, userId, session);
        sessionsById.put(session.getId(), info);
        sessionIdsByUser.computeIfAbsent(new UserKey(tenantId, userId), ignored -> ConcurrentHashMap.newKeySet())
                .add(session.getId());
    }

    public void unregister(String sessionId) {
        WebSocketSessionInfo info = sessionsById.remove(sessionId);
        if (info == null) {
            return;
        }
        UserKey key = new UserKey(info.getTenantId(), info.getUserId());
        Set<String> sessionIds = sessionIdsByUser.get(key);
        if (sessionIds == null) {
            return;
        }
        sessionIds.remove(sessionId);
        if (sessionIds.isEmpty()) {
            sessionIdsByUser.remove(key);
        }
    }

    public boolean hasSessions(Long tenantId, Long userId) {
        Set<String> sessionIds = sessionIdsByUser.get(new UserKey(tenantId, userId));
        return sessionIds != null && !sessionIds.isEmpty();
    }

    public Collection<WebSocketSession> getSessions(Long tenantId, Long userId) {
        Set<String> sessionIds = sessionIdsByUser.get(new UserKey(tenantId, userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<WebSocketSession> sessions = new ArrayList<>(sessionIds.size());
        for (String sessionId : sessionIds) {
            WebSocketSessionInfo info = sessionsById.get(sessionId);
            if (info != null && info.getSession().isOpen()) {
                sessions.add(info.getSession());
            }
        }
        return sessions;
    }

    public WebSocketSession getSession(String sessionId) {
        WebSocketSessionInfo info = sessionsById.get(sessionId);
        return info != null ? info.getSession() : null;
    }

    private record UserKey(Long tenantId, Long userId) {
    }
}
