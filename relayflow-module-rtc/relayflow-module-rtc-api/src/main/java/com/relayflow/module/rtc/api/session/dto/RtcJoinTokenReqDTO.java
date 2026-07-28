package com.relayflow.module.rtc.api.session.dto;

import lombok.Data;

@Data
public class RtcJoinTokenReqDTO {

    private Long sessionId;

    private Long userId;

    /** Display name in LiveKit room */
    private String displayName;

    /** Allow publishing tracks (default true) */
    private Boolean canPublish = true;

    /** Allow subscribing (default true) */
    private Boolean canSubscribe = true;

    /** Token TTL seconds (default from config) */
    private Long ttlSeconds;
}
