package com.relayflow.module.calendar.enums;

/**
 * Calendar container type.
 */
public enum CalendarType {

    PRIMARY,
    OWNED,
    /** Response-only: calendar shared to current user (not stored on cal_calendar.type). */
    SHARED
}
