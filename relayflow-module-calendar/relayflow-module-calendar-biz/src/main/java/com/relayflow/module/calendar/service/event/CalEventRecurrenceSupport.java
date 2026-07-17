package com.relayflow.module.calendar.service.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.calendar.dal.dataobject.CalEventDO;
import com.relayflow.module.calendar.dal.dataobject.CalEventExceptionDO;
import com.relayflow.module.calendar.dal.mapper.CalEventExceptionMapper;
import com.relayflow.module.calendar.dal.mapper.CalEventMapper;
import com.relayflow.module.calendar.enums.CalendarEventStatus;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import com.relayflow.module.calendar.service.rrule.RecurrenceExpander;
import com.relayflow.module.calendar.service.rrule.RecurrenceExpander.Occurrence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recurrence expansion / exception helpers extracted from {@link CalEventServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class CalEventRecurrenceSupport {

    static final int CANCELLED = 1;
    static final int NOT_CANCELLED = 0;

    enum EditScope {
        THIS, ALL, THIS_AND_FUTURE
    }

    record ExpandedInstance(OffsetDateTime instanceStart, OffsetDateTime instanceEnd,
                            CalEventExceptionDO exception) {
    }

    private final CalEventMapper calEventMapper;
    private final CalEventExceptionMapper calEventExceptionMapper;

    List<CalEventDO> selectNonRecurringOverlapping(Set<Long> calendarIds, OffsetDateTime from, OffsetDateTime to) {
        return calEventMapper.selectList(
                Wrappers.<CalEventDO>lambdaQuery()
                        .in(CalEventDO::getCalendarId, calendarIds)
                        .eq(CalEventDO::getStatus, CalendarEventStatus.CONFIRMED.name())
                        .isNull(CalEventDO::getRrule)
                        .lt(CalEventDO::getStartTime, to)
                        .gt(CalEventDO::getEndTime, from));
    }

    List<CalEventDO> selectRecurringMasters(Set<Long> calendarIds, OffsetDateTime to) {
        return calEventMapper.selectList(
                Wrappers.<CalEventDO>lambdaQuery()
                        .in(CalEventDO::getCalendarId, calendarIds)
                        .eq(CalEventDO::getStatus, CalendarEventStatus.CONFIRMED.name())
                        .isNotNull(CalEventDO::getRrule)
                        .lt(CalEventDO::getStartTime, to));
    }

    boolean matchesListWindow(CalEventDO event, OffsetDateTime from, OffsetDateTime to) {
        if (!StringUtils.hasText(event.getRrule())) {
            return event.getStartTime().isBefore(to) && event.getEndTime().isAfter(from);
        }
        return event.getStartTime().isBefore(to);
    }

    List<ExpandedInstance> expandMaster(CalEventDO master, OffsetDateTime from, OffsetDateTime to,
                                                List<CalEventExceptionDO> exceptions) {
        Map<OffsetDateTime, CalEventExceptionDO> exceptionByStart = exceptions.stream()
                .collect(Collectors.toMap(CalEventExceptionDO::getOriginalStart, e -> e, (a, b) -> a, LinkedHashMap::new));

        if (!StringUtils.hasText(master.getRrule())) {
            CalEventExceptionDO ex = exceptionByStart.get(master.getStartTime());
            if (ex != null && Objects.equals(ex.getCancelled(), CANCELLED)) {
                return List.of();
            }
            OffsetDateTime start = master.getStartTime();
            OffsetDateTime end = master.getEndTime();
            if (ex != null) {
                if (ex.getOverrideStart() != null) {
                    start = ex.getOverrideStart();
                }
                if (ex.getOverrideEnd() != null) {
                    end = ex.getOverrideEnd();
                }
            }
            if (start.isBefore(to) && end.isAfter(from)) {
                return List.of(new ExpandedInstance(start, end, ex));
            }
            return List.of();
        }

        List<Occurrence> occurrences = RecurrenceExpander.expand(
                master.getStartTime(), master.getEndTime(), master.getRrule(), from, to);
        List<ExpandedInstance> result = new ArrayList<>(occurrences.size());
        for (Occurrence occurrence : occurrences) {
            CalEventExceptionDO ex = exceptionByStart.get(occurrence.start());
            if (ex != null && Objects.equals(ex.getCancelled(), CANCELLED)) {
                continue;
            }
            OffsetDateTime start = occurrence.start();
            OffsetDateTime end = occurrence.end();
            if (ex != null) {
                if (ex.getOverrideStart() != null) {
                    start = ex.getOverrideStart();
                }
                if (ex.getOverrideEnd() != null) {
                    end = ex.getOverrideEnd();
                }
            }
            if (start.isBefore(to) && end.isAfter(from)) {
                result.add(new ExpandedInstance(occurrence.start(), end, ex));
            }
        }
        return result;
    }

    Map<Long, List<CalEventExceptionDO>> loadExceptions(Set<Long> masterIds) {
        if (masterIds.isEmpty()) {
            return Map.of();
        }
        List<CalEventExceptionDO> rows = calEventExceptionMapper.selectList(
                Wrappers.<CalEventExceptionDO>lambdaQuery()
                        .in(CalEventExceptionDO::getMasterEventId, masterIds));
        Map<Long, List<CalEventExceptionDO>> map = new HashMap<>();
        for (CalEventExceptionDO row : rows) {
            map.computeIfAbsent(row.getMasterEventId(), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    void upsertException(Long masterEventId, OffsetDateTime originalStart, Integer cancelled,
                                 String overrideTitle, OffsetDateTime overrideStart, OffsetDateTime overrideEnd,
                                 Long userId, Long tenantId) {
        if (originalStart == null) {
            throw new ServiceException(ErrorCodeConstants.EVENT_EDIT_SCOPE_INVALID);
        }
        CalEventExceptionDO existing = calEventExceptionMapper.selectOne(
                Wrappers.<CalEventExceptionDO>lambdaQuery()
                        .eq(CalEventExceptionDO::getMasterEventId, masterEventId)
                        .eq(CalEventExceptionDO::getOriginalStart, originalStart)
                        .last("LIMIT 1"));
        OffsetDateTime now = OffsetDateTime.now();
        if (existing != null) {
            existing.setCancelled(cancelled);
            existing.setOverrideTitle(overrideTitle);
            existing.setOverrideStart(overrideStart);
            existing.setOverrideEnd(overrideEnd);
            existing.setUpdater(userId);
            existing.setUpdateTime(now);
            calEventExceptionMapper.updateById(existing);
            return;
        }
        CalEventExceptionDO row = new CalEventExceptionDO();
        row.setTenantId(tenantId);
        row.setMasterEventId(masterEventId);
        row.setOriginalStart(originalStart);
        row.setCancelled(cancelled);
        row.setOverrideTitle(overrideTitle);
        row.setOverrideStart(overrideStart);
        row.setOverrideEnd(overrideEnd);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        calEventExceptionMapper.insert(row);
    }

    void truncateSeriesUntil(CalEventDO event, OffsetDateTime instanceStart, Long userId) {
        if (!StringUtils.hasText(event.getRrule())) {
            throw new ServiceException(ErrorCodeConstants.EVENT_EDIT_SCOPE_INVALID);
        }
        OffsetDateTime until = instanceStart.minusSeconds(1);
        event.setRrule(truncateRruleUntil(event.getRrule(), until));
        event.setUpdater(userId);
        event.setUpdateTime(OffsetDateTime.now());
        calEventMapper.updateById(event);
    }

    EditScope parseEditScope(String scopeRaw, CalEventDO event, OffsetDateTime instanceStart,
                                     EditScope defaultWithInstance) {
        if (!StringUtils.hasText(event.getRrule())) {
            return EditScope.ALL;
        }
        if (StringUtils.hasText(scopeRaw)) {
            String scope = scopeRaw.trim().toUpperCase(Locale.ROOT);
            if ("ALL".equals(scope)) {
                return EditScope.ALL;
            }
            if ("THIS".equals(scope)) {
                if (instanceStart == null) {
                    throw new ServiceException(ErrorCodeConstants.EVENT_EDIT_SCOPE_INVALID);
                }
                return EditScope.THIS;
            }
            if ("THIS_AND_FUTURE".equals(scope)) {
                if (instanceStart == null) {
                    throw new ServiceException(ErrorCodeConstants.EVENT_EDIT_SCOPE_INVALID);
                }
                return EditScope.THIS_AND_FUTURE;
            }
            throw new ServiceException(ErrorCodeConstants.EVENT_EDIT_SCOPE_INVALID);
        }
        if (instanceStart != null) {
            return defaultWithInstance;
        }
        return EditScope.ALL;
    }

    static String truncateRruleUntil(String rrule, OffsetDateTime until) {
        String body = rrule.trim();
        if (body.toUpperCase(Locale.ROOT).startsWith("RRULE:")) {
            body = body.substring(6);
        }
        StringBuilder sb = new StringBuilder();
        for (String token : body.split(";")) {
            if (!StringUtils.hasText(token) || !token.contains("=")) {
                continue;
            }
            String key = token.split("=", 2)[0].trim().toUpperCase(Locale.ROOT);
            if ("COUNT".equals(key) || "UNTIL".equals(key)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(token.trim());
        }
        if (sb.length() > 0) {
            sb.append(';');
        }
        sb.append("UNTIL=").append(formatUntil(until));
        return sb.toString();
    }

    static String formatUntil(OffsetDateTime until) {
        return until.withOffsetSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    static String normalizeRrule(String rrule) {
        if (!StringUtils.hasText(rrule)) {
            return null;
        }
        return rrule.trim();
    }

}
