package com.relayflow.framework.security.core;

import java.time.Duration;

/**
 * JWT 吊销存储 SPI，由 Redis 等实现提供。
 */
public interface TokenRevocationStore {

    void revoke(String jti, Long tenantId, Duration ttl);

    boolean isRevoked(String jti, Long tenantId);
}
