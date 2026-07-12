package com.relayflow.module.system.api.tenant;

import java.util.Collection;
import java.util.Set;

public interface TenantMemberApi {

    Set<Long> filterActiveMemberUserIds(Long tenantId, Collection<Long> userIds);
}
