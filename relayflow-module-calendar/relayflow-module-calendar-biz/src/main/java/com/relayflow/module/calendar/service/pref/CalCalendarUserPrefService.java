package com.relayflow.module.calendar.service.pref;

import java.util.Collection;
import java.util.Map;

public interface CalCalendarUserPrefService {

    /** calendarId → displayColor (only rows with non-null color). */
    Map<Long, String> displayColors(Long userId, Collection<Long> calendarIds);

    /** Upsert or clear personal display color for a calendar visible to the current user. */
    void updateDisplayColor(Long calendarId, String displayColor);
}
