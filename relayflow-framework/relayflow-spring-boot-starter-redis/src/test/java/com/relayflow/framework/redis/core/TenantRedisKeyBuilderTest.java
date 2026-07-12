package com.relayflow.framework.redis.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantRedisKeyBuilderTest {

    @Test
    void buildsTenantScopedKey() {
        assertEquals("t:1:auth:perms:42", TenantRedisKeyBuilder.build(1L, "auth:perms", "42"));
    }

    @Test
    void rejectsNullTenantId() {
        assertThrows(IllegalArgumentException.class,
                () -> TenantRedisKeyBuilder.build(null, "ws", "fanout"));
    }
}
