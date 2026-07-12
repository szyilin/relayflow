package com.relayflow.module.infra.websocket.handler;

import com.relayflow.framework.websocket.core.WebSocketInstanceIdProvider;
import com.relayflow.framework.websocket.core.WebSocketOnlineService;
import com.relayflow.module.infra.api.realtime.RealtimeDomainMessageHandler;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;
import com.relayflow.module.infra.api.realtime.enums.RealtimeDomain;
import com.relayflow.module.infra.api.realtime.enums.RealtimeSystemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemPingPongHandler implements RealtimeDomainMessageHandler {

    private final WebSocketOnlineService onlineService;
    private final WebSocketInstanceIdProvider instanceIdProvider;

    @Override
    public String domain() {
        return RealtimeDomain.SYSTEM.getCode();
    }

    @Override
    public void onMessage(RealtimeEnvelopeDTO envelope, RealtimeSessionContext session, RealtimeSessionSender sessionSender) {
        if (!RealtimeSystemType.PING.getCode().equals(envelope.getType())) {
            return;
        }
        onlineService.refreshOnline(session.tenantId(), session.userId(), instanceIdProvider.getInstanceId());
        RealtimeEnvelopeDTO pong = RealtimeEnvelopeDTO.builder()
                .domain(RealtimeDomain.SYSTEM.getCode())
                .type(RealtimeSystemType.PONG.getCode())
                .requestId(envelope.getRequestId())
                .ts(System.currentTimeMillis())
                .payload(envelope.getPayload())
                .build();
        sessionSender.send(session, pong);
    }
}
