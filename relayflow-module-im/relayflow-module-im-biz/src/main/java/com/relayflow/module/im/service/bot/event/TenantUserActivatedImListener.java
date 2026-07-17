package com.relayflow.module.im.service.bot.event;

import com.relayflow.framework.messaging.DomainEvent;
import com.relayflow.framework.messaging.DomainEventHandler;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.system.api.event.SystemDomainEventTypes;
import com.relayflow.module.system.api.event.TenantUserActivatedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantUserActivatedImListener implements DomainEventHandler<TenantUserActivatedPayload> {

    private final ImBotApi imBotApi;

    @Override
    public String eventType() {
        return SystemDomainEventTypes.TENANT_USER_ACTIVATED;
    }

    @Override
    public Class<TenantUserActivatedPayload> payloadType() {
        return TenantUserActivatedPayload.class;
    }

    @Override
    public void handle(DomainEvent envelope, TenantUserActivatedPayload payload) {
        if (payload == null || payload.getTenantId() == null || payload.getUserId() == null) {
            log.warn("Ignore tenant_user.activated with incomplete payload: eventId={}",
                    envelope != null ? envelope.getEventId() : null);
            return;
        }
        imBotApi.ensureUserEnablementsOnActive(payload.getTenantId(), payload.getUserId());
    }
}
