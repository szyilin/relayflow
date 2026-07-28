package com.relayflow.module.meeting.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MeetingSummaryRespVO {

    private Long id;

    private String meetingCode;

    private String title;

    private String status;

    private Long hostUserId;

    private OffsetDateTime scheduledStart;

    private OffsetDateTime scheduledEnd;

    private Long calendarEventId;

    private OffsetDateTime createTime;
}
