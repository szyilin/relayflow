package com.relayflow.module.meeting.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MeetingScheduleReqVO {

    private String title;

    @NotNull
    private OffsetDateTime scheduledStart;

    @NotNull
    private OffsetDateTime scheduledEnd;
}
