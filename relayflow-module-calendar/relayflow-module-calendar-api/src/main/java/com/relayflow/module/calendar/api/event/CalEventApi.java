package com.relayflow.module.calendar.api.event;

import com.relayflow.module.calendar.api.event.dto.CalEventLinkedCreateReqDTO;

/**
 * Cross-domain calendar event API. Callers must not write {@code cal_*} via mappers.
 */
public interface CalEventApi {

    /**
     * Create an event on the organizer's primary calendar linked to an opaque meeting id.
     *
     * @return created {@code cal_event.id}
     */
    Long createLinkedEvent(CalEventLinkedCreateReqDTO request);

    /**
     * Best-effort clear of {@code meeting_id} for events linked to the given meeting.
     */
    void clearMeetingLink(Long meetingId);
}
