package com.relayflow.module.calendar.controller.app.vo;

import lombok.Data;

@Data
public class CalCalendarRespVO {

    private Long id;

    private String name;

    private String color;

    private String description;

    /** PRIMARY | OWNED | SHARED */
    private String type;

    private Long ownerUserId;

    /** Present when type=SHARED */
    private String permission;
}
