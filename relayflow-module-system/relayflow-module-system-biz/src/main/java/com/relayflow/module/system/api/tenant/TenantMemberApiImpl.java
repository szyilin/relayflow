package com.relayflow.module.system.api.tenant;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantMemberApiImpl implements TenantMemberApi {

    private final SysTenantUserMapper tenantUserMapper;

    @Override
    public Set<Long> filterActiveMemberUserIds(Long tenantId, Collection<Long> userIds) {
        if (tenantId == null || CollectionUtils.isEmpty(userIds)) {
            return Set.of();
        }
        Set<Long> requested = new HashSet<>();
        for (Long userId : userIds) {
            if (userId != null && userId > 0) {
                requested.add(userId);
            }
        }
        if (requested.isEmpty()) {
            return Set.of();
        }

        return tenantUserMapper.selectList(
                        Wrappers.<SysTenantUserDO>lambdaQuery()
                                .eq(SysTenantUserDO::getTenantId, tenantId)
                                .in(SysTenantUserDO::getUserId, requested)
                                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE))
                .stream()
                .map(SysTenantUserDO::getUserId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Long> listActiveTenantIds(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        return tenantUserMapper.selectList(
                        Wrappers.<SysTenantUserDO>lambdaQuery()
                                .eq(SysTenantUserDO::getUserId, userId)
                                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE)
                                .orderByAsc(SysTenantUserDO::getTenantId))
                .stream()
                .map(SysTenantUserDO::getTenantId)
                .distinct()
                .toList();
    }
}
