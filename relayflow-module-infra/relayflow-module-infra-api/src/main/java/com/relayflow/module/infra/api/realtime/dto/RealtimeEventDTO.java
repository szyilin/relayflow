package com.relayflow.module.infra.api.realtime.dto;

import lombok.Builder;

import java.util.Collection;

@Builder
public record RealtimeEventDTO(
        String domain,
        String type,
        Long tenantId,
        Collection<Long> targetUserIds,
        Object payload
) {
}
