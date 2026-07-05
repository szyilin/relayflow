package com.relayflow.module.system.service.permission;

import com.relayflow.framework.security.core.PermissionAuthoritiesLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionAuthoritiesLoaderImpl implements PermissionAuthoritiesLoader {

    private final PermissionService permissionService;

    @Override
    public Set<String> loadPermissionCodes(Long userId, Long tenantId) {
        return permissionService.getPermissionCodes(userId, tenantId);
    }
}
