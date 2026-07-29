package com.relayflow.module.calendar.service.allstaff;

import com.relayflow.module.calendar.controller.admin.vo.CalAllStaffCalendarRespVO;
import com.relayflow.module.calendar.controller.admin.vo.CalAllStaffSaveReqVO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;

import java.util.List;
import java.util.Set;

public interface CalAllStaffService {

    List<CalAllStaffCalendarRespVO> listAdmin();

    Long create(CalAllStaffSaveReqVO request);

    void update(CalAllStaffSaveReqVO request);

    void delete(Long id);

    /** Calendar ids of ALL_STAFF calendars visible to user (scope match). */
    Set<Long> visibleCalendarIds(Long userId);

    List<CalCalendarDO> loadVisibleCalendars(Long userId);

    boolean isVisibleToUser(Long calendarId, Long userId);
}
