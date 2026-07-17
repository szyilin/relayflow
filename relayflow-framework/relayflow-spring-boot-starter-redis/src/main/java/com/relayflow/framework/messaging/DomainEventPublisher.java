package com.relayflow.framework.messaging;

/**
 * Publishes domain events. Prefer calling after local business writes succeed;
 * when a Spring transaction is active, the Redis write is deferred until after commit.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
