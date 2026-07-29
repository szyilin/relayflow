package com.relayflow.module.calendar.controller.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CalProductSettingsRespVO {

    /** Default true when no row. */
    private Boolean allowMemberCreateOwnedCalendar = true;

    private List<CalAllStaffCalendarRespVO> allStaffCalendars = new ArrayList<>();
}
