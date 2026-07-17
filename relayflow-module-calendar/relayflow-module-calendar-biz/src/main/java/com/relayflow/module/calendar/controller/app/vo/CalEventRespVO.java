package com.relayflow.module.calendar.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class CalEventRespVO {

    private Long id;

    private Long calendarId;

    private String calendarColor;

    private String calendarName;

    private String title;

    private String description;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private Boolean allDay;

    private Long organizerId;

    private Integer remindBeforeMinutes;

    private String allDayRemindTime;

    /** CONFIRMED | CANCELLED */
    private String status;

    /** ORGANIZER | ATTENDEE for current viewer */
    private String viewerRole;

    private List<CalAttendeeRespVO> attendees;

    /** true when viewer does not own the calendar and is not a share reader (invited-only) */
    private Boolean invitedOnly;

    private String rrule;

    /** Master event id (= id for non-recurring / master row) */
    private Long masterEventId;

    /** Expanded instance start; equals startTime for single events */
    private OffsetDateTime instanceStart;

    private Boolean isException;

    private Boolean recurring;
}
