package com.relayflow.module.infra.api.realtime;

import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;

import java.util.Collection;

public interface RealtimeTransportApi {

    void sendToUser(Long tenantId, Long userId, RealtimeEnvelopeDTO envelope);

    void sendToUsers(Long tenantId, Collection<Long> userIds, RealtimeEnvelopeDTO envelope);

    boolean isUserOnline(Long tenantId, Long userId);
}
