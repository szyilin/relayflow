package com.relayflow.module.infra.api.realtime;

import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEventDTO;
import com.relayflow.module.infra.api.realtime.enums.RealtimeDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventPublisherImpl implements RealtimeEventPublisher {

    private final RealtimeTransportApi realtimeTransportApi;

    @Override
    public void publish(RealtimeEventDTO event) {
        if (event == null || event.domain() == null) {
            return;
        }
        RealtimeDomain domain = RealtimeDomain.fromCode(event.domain());
        if (domain == null) {
            log.debug("Ignore unknown realtime event domain={}", event.domain());
            return;
        }
        switch (domain) {
            case NOTIFY, PRESENCE -> log.debug("No-op realtime event domain={} type={}", event.domain(), event.type());
            case IM -> log.debug("IM realtime event domain={} type={} awaiting im module handler registration", event.domain(), event.type());
            case SYSTEM -> pushIfTargets(event);
            default -> log.debug("Unhandled realtime event domain={}", event.domain());
        }
    }

    private void pushIfTargets(RealtimeEventDTO event) {
        if (event.tenantId() == null || CollectionUtils.isEmpty(event.targetUserIds())) {
            return;
        }
        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(event.domain())
                .type(event.type())
                .requestId(null)
                .ts(System.currentTimeMillis())
                .payload(event.payload())
                .build();
        realtimeTransportApi.sendToUsers(event.tenantId(), event.targetUserIds(), envelope);
    }
}
