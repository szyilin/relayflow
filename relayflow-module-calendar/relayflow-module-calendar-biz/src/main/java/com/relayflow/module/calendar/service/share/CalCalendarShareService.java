package com.relayflow.module.calendar.service.share;

import com.relayflow.module.calendar.controller.app.vo.CalCalendarShareCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarShareRespVO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CalCalendarShareService {

    List<CalCalendarShareRespVO> listMine();

    Long create(CalCalendarShareCreateReqVO request);

    void delete(Long id);

    /** calendarId -> permission for current user as grantee */
    Map<Long, String> sharedCalendarPermissions(Long userId);

    Set<Long> sharedReadCalendarIds(Long userId);

    List<CalCalendarDO> loadSharedCalendars(Long userId);
}
