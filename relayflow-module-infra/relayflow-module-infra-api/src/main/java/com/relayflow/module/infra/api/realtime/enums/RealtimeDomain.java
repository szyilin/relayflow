package com.relayflow.module.infra.api.realtime.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RealtimeDomain {

    IM("im"),
    NOTIFY("notify"),
    PRESENCE("presence"),
    SYSTEM("system");

    private final String code;

    public static RealtimeDomain fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RealtimeDomain domain : values()) {
            if (domain.code.equalsIgnoreCase(code)) {
                return domain;
            }
        }
        return null;
    }
}
