package com.relayflow.framework.security.core;

/** Authoritative membership lookup. Implementations must reject missing/inactive members. */
public interface MemberIdentityProvider {

    long requireActiveVersion(Long tenantId, Long userId);
}
