package com.relayflow.framework.websocket.core;

import java.util.Collection;

public interface WebSocketMessageSender {

    void send(Long tenantId, Collection<Long> userIds, RealtimeEnvelope envelope);
}
