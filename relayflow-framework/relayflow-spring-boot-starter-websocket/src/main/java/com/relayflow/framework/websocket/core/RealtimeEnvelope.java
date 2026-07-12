package com.relayflow.framework.websocket.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEnvelope {

    private String domain;
    private String type;
    private String requestId;
    private Long ts;
    private Object payload;
}
