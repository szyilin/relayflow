package com.relayflow.module.calendar.api.event.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CalEventLinkedCreateReqDTO {

    private Long tenantId;

    private Long organizerUserId;

    private String title;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    /** Opaque meeting id from the meeting domain; stored as Long only. */
    private Long meetingId;
}
