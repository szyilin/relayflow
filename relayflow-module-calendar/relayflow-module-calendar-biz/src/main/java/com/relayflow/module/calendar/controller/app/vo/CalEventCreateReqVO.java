package com.relayflow.module.calendar.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class CalEventCreateReqVO {

    @NotNull
    private Long calendarId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull
    private OffsetDateTime startTime;

    @NotNull
    private OffsetDateTime endTime;

    @NotNull
    private Boolean allDay;

    private Integer remindBeforeMinutes;

    @Size(max = 8)
    private String allDayRemindTime;

    private List<Long> attendeeUserIds;

    /** RFC5545 RRULE; null/blank = single instance */
    @Size(max = 512)
    private String rrule;
}
