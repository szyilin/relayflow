package com.relayflow.module.calendar.controller.app.vo;

import lombok.Data;

@Data
public class CalAttendeeRespVO {

    private Long userId;

    /** ORGANIZER | ATTENDEE */
    private String role;

    /** NEEDS_ACTION | ACCEPTED | DECLINED */
    private String response;

    private String nickname;
}
