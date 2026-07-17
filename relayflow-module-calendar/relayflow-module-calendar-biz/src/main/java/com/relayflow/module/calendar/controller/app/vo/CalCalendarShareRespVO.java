package com.relayflow.module.calendar.controller.app.vo;

import lombok.Data;

@Data
public class CalCalendarShareRespVO {

    private Long id;

    private Long calendarId;

    private String calendarName;

    private String calendarColor;

    private Long granteeUserId;

    private String granteeNickname;

    private Long ownerUserId;

    private String ownerNickname;

    /** READ */
    private String permission;

    /** OUTGOING | INCOMING */
    private String direction;
}
