package com.relayflow.framework.security.core;

import java.util.Set;

/**
 * Loads permission codes for authenticated users. Implemented by the system module at runtime.
 */
public interface PermissionAuthoritiesLoader {

    Set<String> loadPermissionCodes(Long userId, Long tenantId);
}
