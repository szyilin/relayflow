package com.relayflow.module.calendar.controller.app.vo;

import lombok.Data;

@Data
public class CalCalendarRespVO {

    private Long id;

    private String name;

    /** Calendar default color (entity). */
    private String color;

    private String description;

    /** PRIMARY | OWNED | SHARED | ALL_STAFF (response) */
    private String type;

    private Long ownerUserId;

    /** Present when type=SHARED (or subscribed) */
    private String permission;

    /** Same as color; explicit for clients. */
    private String defaultColor;

    /** Current user's personal override; null if none. */
    private String displayColor;

    /** displayColor ?? defaultColor */
    private String effectiveColor;
}
