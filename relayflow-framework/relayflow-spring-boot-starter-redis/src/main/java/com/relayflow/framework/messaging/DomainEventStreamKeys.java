package com.relayflow.framework.messaging;

final class DomainEventStreamKeys {

    static final String CONSUMER_GROUP = "cg:relayflow";
    static final String IDEMP_PREFIX = "de:idemp:";

    private DomainEventStreamKeys() {
    }

    static String streamKey(String eventType) {
        return "de:" + eventType;
    }

    static String idempotencyKey(String eventId) {
        return IDEMP_PREFIX + eventId;
    }
}
