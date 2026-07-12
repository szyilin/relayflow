package com.relayflow.framework.websocket.sender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisWebSocketMessageSenderTest {

    @Test
    void fanoutChannelIsTenantScoped() {
        assertEquals("t:1:ws:fanout", RedisWebSocketMessageSender.fanoutChannel(1L));
        assertEquals("t:9:ws:fanout", RedisWebSocketMessageSender.fanoutChannel(9L));
    }
}
