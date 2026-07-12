package com.relayflow.module.infra.websocket.support;

import com.relayflow.framework.websocket.core.RealtimeEnvelope;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;

public final class RealtimeEnvelopeConverter {

    private RealtimeEnvelopeConverter() {
    }

    public static RealtimeEnvelope toFramework(RealtimeEnvelopeDTO dto) {
        if (dto == null) {
            return null;
        }
        return RealtimeEnvelope.builder()
                .domain(dto.getDomain())
                .type(dto.getType())
                .requestId(dto.getRequestId())
                .ts(dto.getTs())
                .payload(dto.getPayload())
                .build();
    }

    public static RealtimeEnvelopeDTO toApi(RealtimeEnvelope envelope) {
        if (envelope == null) {
            return null;
        }
        return RealtimeEnvelopeDTO.builder()
                .domain(envelope.getDomain())
                .type(envelope.getType())
                .requestId(envelope.getRequestId())
                .ts(envelope.getTs())
                .payload(envelope.getPayload())
                .build();
    }
}
