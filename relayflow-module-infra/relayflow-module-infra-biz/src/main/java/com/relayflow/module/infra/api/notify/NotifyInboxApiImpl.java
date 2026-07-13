package com.relayflow.module.infra.api.notify;

import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.service.notify.NotifyInboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotifyInboxApiImpl implements NotifyInboxApi {

    private final NotifyInboxService notifyInboxService;

    @Override
    public Long push(NotifyItemCommand command) {
        return notifyInboxService.push(command);
    }

    @Override
    public void backfillUserIdByMobile(String mobile, Long userId) {
        notifyInboxService.backfillUserIdByMobile(mobile, userId);
    }

    @Override
    public boolean hasUnreadDedupe(Long tenantId, Long userId, String type, String dedupeKey) {
        return notifyInboxService.hasUnreadDedupe(tenantId, userId, type, dedupeKey);
    }
}
