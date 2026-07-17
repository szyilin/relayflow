package com.relayflow.framework.messaging;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Cross-domain domain-event envelope (see docs/dev/cross-domain-messaging.md).
 */
@Data
@Builder
public class DomainEvent {

    private String eventId;
    private String eventType;
    private Instant occurredAt;
    private Long tenantId;
    private String producer;
    private int schemaVersion;
    /** JSON-serializable DTO from the producing module's *-api. */
    private Object payload;
}
