package com.relayflow.module.infra.api.realtime;

import com.relayflow.framework.websocket.core.WebSocketMessageSender;
import com.relayflow.framework.websocket.core.WebSocketOnlineService;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.websocket.support.RealtimeEnvelopeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeTransportApiImpl implements RealtimeTransportApi {

    private final WebSocketMessageSender webSocketMessageSender;
    private final WebSocketOnlineService onlineService;

    @Override
    public void sendToUser(Long tenantId, Long userId, RealtimeEnvelopeDTO envelope) {
        sendToUsers(tenantId, List.of(userId), envelope);
    }

    @Override
    public void sendToUsers(Long tenantId, Collection<Long> userIds, RealtimeEnvelopeDTO envelope) {
        if (tenantId == null || userIds == null || userIds.isEmpty() || envelope == null) {
            return;
        }
        webSocketMessageSender.send(tenantId, userIds, RealtimeEnvelopeConverter.toFramework(envelope));
    }

    @Override
    public boolean isUserOnline(Long tenantId, Long userId) {
        return onlineService.isOnline(tenantId, userId);
    }
}
