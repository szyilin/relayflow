package com.relayflow.module.calendar.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalCalendarShareCreateReqVO {

    @NotNull
    private Long calendarId;

    @NotNull
    private Long granteeUserId;

    /** READ (default) */
    private String permission;
}
