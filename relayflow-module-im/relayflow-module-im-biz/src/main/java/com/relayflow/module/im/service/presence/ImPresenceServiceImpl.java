package com.relayflow.module.im.service.presence;

import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.PresenceBatchRespVO;
import com.relayflow.module.im.controller.app.vo.PresenceItemRespVO;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImPresenceServiceImpl implements ImPresenceService {

    private static final int MAX_BATCH_SIZE = 50;

    private final TenantMemberApi tenantMemberApi;
    private final RealtimeTransportApi realtimeTransportApi;

    @Override
    public PresenceBatchRespVO batchPresence(List<Long> userIds) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        List<Long> requested = userIds == null ? List.of() : userIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .limit(MAX_BATCH_SIZE)
                .toList();

        Set<Long> memberIds = tenantMemberApi.filterActiveMemberUserIds(tenantId, requested);
        List<PresenceItemRespVO> items = new ArrayList<>();
        for (Long userId : requested) {
            if (!memberIds.contains(userId)) {
                continue;
            }
            PresenceItemRespVO item = new PresenceItemRespVO();
            item.setUserId(userId);
            item.setOnline(realtimeTransportApi.isUserOnline(tenantId, userId));
            items.add(item);
        }

        PresenceBatchRespVO response = new PresenceBatchRespVO();
        response.setItems(items);
        return response;
    }
}
