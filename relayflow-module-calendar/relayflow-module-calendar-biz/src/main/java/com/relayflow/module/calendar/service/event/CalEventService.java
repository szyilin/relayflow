package com.relayflow.module.calendar.service.event;

import com.relayflow.module.calendar.controller.app.vo.CalEventCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRescheduleReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRespondReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventUpdateReqVO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public interface CalEventService {

    List<CalEventRespVO> list(OffsetDateTime from, OffsetDateTime to, Set<Long> calendarIdsFilter);

    CalEventRespVO get(Long id);

    Long create(CalEventCreateReqVO request);

    void update(CalEventUpdateReqVO request);

    void delete(Long id, String editScope, OffsetDateTime instanceStart);

    void reschedule(CalEventRescheduleReqVO request);

    void respond(CalEventRespondReqVO request);
}
