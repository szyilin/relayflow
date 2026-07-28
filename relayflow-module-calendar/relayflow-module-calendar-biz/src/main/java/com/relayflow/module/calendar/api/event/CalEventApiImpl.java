package com.relayflow.module.calendar.api.event;

import com.relayflow.module.calendar.api.event.dto.CalEventLinkedCreateReqDTO;
import com.relayflow.module.calendar.service.event.CalEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalEventApiImpl implements CalEventApi {

    private final CalEventService calEventService;

    @Override
    public Long createLinkedEvent(CalEventLinkedCreateReqDTO request) {
        return calEventService.createLinkedEvent(request);
    }

    @Override
    public void clearMeetingLink(Long meetingId) {
        calEventService.clearMeetingLink(meetingId);
    }
}
