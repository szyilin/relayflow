package com.relayflow.framework.security.core;

import java.time.Instant;

/** Verified JWT metadata only; never retains the bearer token. */
public record SessionIdentity(Long tenantId, Long userId, String userType, String jti,
                              Instant expiresAt, long memberVersion) {
}
