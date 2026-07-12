package com.relayflow.module.infra.api.realtime.dto;

public record RealtimeSessionContext(Long tenantId, Long userId, String sessionId) {
}
