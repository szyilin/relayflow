package com.relayflow.module.infra.api.realtime.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RealtimeSystemType {

    PING("ping"),
    PONG("pong");

    private final String code;
}
