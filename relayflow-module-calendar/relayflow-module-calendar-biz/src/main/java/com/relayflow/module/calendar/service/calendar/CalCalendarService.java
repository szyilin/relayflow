package com.relayflow.module.calendar.service.calendar;

import com.relayflow.module.calendar.controller.app.vo.CalCalendarCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarUpdateReqVO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;

import java.util.List;

public interface CalCalendarService {

    List<CalCalendarRespVO> listMine();

    /** Owned calendars plus calendars shared with the current user. */
    List<CalCalendarRespVO> listVisible();

    Long create(CalCalendarCreateReqVO request);

    void update(CalCalendarUpdateReqVO request);

    void delete(Long id);

    /** Ensure PRIMARY exists for current user; returns DO. */
    CalCalendarDO ensurePrimary(Long tenantId, Long userId);

    CalCalendarDO requireOwnedCalendar(Long calendarId, Long userId);
}
