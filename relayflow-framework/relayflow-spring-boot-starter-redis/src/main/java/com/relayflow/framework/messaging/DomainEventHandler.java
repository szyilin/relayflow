package com.relayflow.framework.messaging;

/**
 * Typed domain-event consumer. Implementations are Spring beans discovered at startup.
 */
public interface DomainEventHandler<T> {

    String eventType();

    Class<T> payloadType();

    void handle(DomainEvent envelope, T payload);
}
