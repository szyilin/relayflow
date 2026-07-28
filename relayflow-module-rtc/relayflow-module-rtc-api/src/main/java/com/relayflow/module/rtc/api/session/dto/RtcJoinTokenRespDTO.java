package com.relayflow.module.rtc.api.session.dto;

import lombok.Data;

@Data
public class RtcJoinTokenRespDTO {

    private Long sessionId;

    private String roomName;

    private String livekitUrl;

    private String token;

    private String identity;

    private Long expiresAtEpochSeconds;
}
