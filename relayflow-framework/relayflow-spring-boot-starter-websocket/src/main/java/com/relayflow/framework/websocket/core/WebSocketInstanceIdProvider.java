package com.relayflow.framework.websocket.core;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WebSocketInstanceIdProvider {

    private final String instanceId = UUID.randomUUID().toString();

    public String getInstanceId() {
        return instanceId;
    }
}
