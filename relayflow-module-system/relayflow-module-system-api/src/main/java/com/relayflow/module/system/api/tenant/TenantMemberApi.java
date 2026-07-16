package com.relayflow.module.system.api.tenant;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TenantMemberApi {

    Set<Long> filterActiveMemberUserIds(Long tenantId, Collection<Long> userIds);

    /**
     * ACTIVE tenant ids where the user is a member (for Bot fanout).
     */
    List<Long> listActiveTenantIds(Long userId);
}
