package com.relayflow.module.rtc.api.session.dto;

import lombok.Data;

@Data
public class RtcSessionRespDTO {

    private Long id;

    private String purpose;

    private String ownerType;

    private Long ownerId;

    private String roomName;

    private String status;

    private Integer participantCount;
}
