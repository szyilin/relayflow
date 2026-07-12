package com.relayflow.module.infra.api.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEnvelopeDTO {

    private String domain;
    private String type;
    private String requestId;
    private Long ts;
    private Object payload;
}
