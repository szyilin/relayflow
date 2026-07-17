package com.relayflow.module.calendar.service.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.app.vo.CalAttendeeRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRescheduleReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRespondReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventUpdateReqVO;
import com.relayflow.module.calendar.dal.dataobject.CalAttendeeDO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.dataobject.CalEventDO;
import com.relayflow.module.calendar.dal.dataobject.CalEventExceptionDO;
import com.relayflow.module.calendar.dal.mapper.CalAttendeeMapper;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalEventMapper;
import com.relayflow.module.calendar.enums.CalendarAttendeeResponse;
import com.relayflow.module.calendar.enums.CalendarAttendeeRole;
import com.relayflow.module.calendar.enums.CalendarEventStatus;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import com.relayflow.module.calendar.service.calendar.CalCalendarService;
import com.relayflow.module.calendar.service.notify.CalendarBotNotifyService;
import com.relayflow.module.calendar.service.rrule.RecurrenceExpander;
import com.relayflow.module.calendar.service.share.CalCalendarShareService;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalEventServiceImpl implements CalEventService {

    private final CalEventMapper calEventMapper;
    private final CalAttendeeMapper calAttendeeMapper;
    private final CalCalendarMapper calCalendarMapper;
    private final CalCalendarService calCalendarService;
    private final CalCalendarShareService calCalendarShareService;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;
    private final CalendarBotNotifyService calendarBotNotifyService;
    private final CalEventRecurrenceSupport recurrenceSupport;

    @Override
    public List<CalEventRespVO> list(OffsetDateTime from, OffsetDateTime to, Set<Long> calendarIdsFilter) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ServiceException(ErrorCodeConstants.EVENT_TIME_INVALID);
        }
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        calCalendarService.ensurePrimary(tenantId, userId);

        Set<Long> ownedIds = ownedCalendarIds(userId);
        Set<Long> sharedReadIds = calCalendarShareService.sharedReadCalendarIds(userId);

        Set<Long> readableCalendarIds = new LinkedHashSet<>(ownedIds);
        readableCalendarIds.addAll(sharedReadIds);

        Set<Long> calendarQueryIds = readableCalendarIds;
        if (calendarIdsFilter != null && !calendarIdsFilter.isEmpty()) {
            calendarQueryIds = readableCalendarIds.stream()
                    .filter(calendarIdsFilter::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        Map<Long, CalEventDO> mastersById = new LinkedHashMap<>();

        if (!calendarQueryIds.isEmpty()) {
            for (CalEventDO event : recurrenceSupport.selectNonRecurringOverlapping(calendarQueryIds, from, to)) {
                mastersById.put(event.getId(), event);
            }
            for (CalEventDO event : recurrenceSupport.selectRecurringMasters(calendarQueryIds, to)) {
                mastersById.put(event.getId(), event);
            }
        }

        List<CalAttendeeDO> myAttend = calAttendeeMapper.selectList(
                Wrappers.<CalAttendeeDO>lambdaQuery().eq(CalAttendeeDO::getUserId, userId));
        Set<Long> attendEventIds = myAttend.stream()
                .map(CalAttendeeDO::getEventId)
                .filter(Objects::nonNull)
                .filter(id -> !mastersById.containsKey(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!attendEventIds.isEmpty()) {
            List<CalEventDO> invited = calEventMapper.selectList(
                    Wrappers.<CalEventDO>lambdaQuery()
                            .in(CalEventDO::getId, attendEventIds)
                            .eq(CalEventDO::getStatus, CalendarEventStatus.CONFIRMED.name()));
            for (CalEventDO event : invited) {
                if (recurrenceSupport.matchesListWindow(event, from, to)) {
                    mastersById.putIfAbsent(event.getId(), event);
                }
            }
        }

        List<CalEventDO> masters = new ArrayList<>(mastersById.values());
        Map<Long, CalCalendarDO> calendarMap = loadCalendars(masters);
        Map<Long, List<CalAttendeeDO>> attendeesByEvent = loadAttendees(masters);
        Map<Long, List<CalEventExceptionDO>> exceptionsByMaster = recurrenceSupport.loadExceptions(mastersById.keySet());

        List<CalEventRespVO> result = new ArrayList<>();
        for (CalEventDO master : masters) {
            List<CalEventRecurrenceSupport.ExpandedInstance> instances = recurrenceSupport.expandMaster(
                    master, from, to, exceptionsByMaster.getOrDefault(master.getId(), List.of()));
            for (CalEventRecurrenceSupport.ExpandedInstance instance : instances) {
                result.add(toResp(master, instance, userId, ownedIds, sharedReadIds,
                        calendarMap, attendeesByEvent));
            }
            calendarBotNotifyService.pushRemindIfDue(master, userId);
        }

        result.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        return result;
    }

    @Override
    public CalEventRespVO get(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalEventDO event = requireVisibleEvent(id, userId);
        Set<Long> ownedIds = ownedCalendarIds(userId);
        Set<Long> sharedReadIds = calCalendarShareService.sharedReadCalendarIds(userId);
        Map<Long, CalCalendarDO> calendarMap = loadCalendars(List.of(event));
        Map<Long, List<CalAttendeeDO>> attendeesByEvent = loadAttendees(List.of(event));
        CalEventRecurrenceSupport.ExpandedInstance instance = new CalEventRecurrenceSupport.ExpandedInstance(event.getStartTime(), event.getEndTime(), null);
        return toResp(event, instance, userId, ownedIds, sharedReadIds, calendarMap, attendeesByEvent);
    }

    @Override
    @Transactional
    public Long create(CalEventCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        validateTime(request.getStartTime(), request.getEndTime());
        CalCalendarDO calendar = calCalendarService.requireOwnedCalendar(request.getCalendarId(), userId);

        String rrule = CalEventRecurrenceSupport.normalizeRrule(request.getRrule());
        RecurrenceExpander.validate(rrule);

        Set<Long> attendeeIds = normalizeAttendeeIds(tenantId, userId, request.getAttendeeUserIds());

        OffsetDateTime now = OffsetDateTime.now();
        CalEventDO event = new CalEventDO();
        event.setTenantId(tenantId);
        event.setCalendarId(calendar.getId());
        event.setTitle(normalizeTitle(request.getTitle()));
        event.setDescription(blankToNull(request.getDescription()));
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setAllDay(Boolean.TRUE.equals(request.getAllDay()) ? 1 : 0);
        event.setOrganizerId(userId);
        event.setRemindBeforeMinutes(request.getRemindBeforeMinutes());
        event.setAllDayRemindTime(blankToNull(request.getAllDayRemindTime()));
        event.setStatus(CalendarEventStatus.CONFIRMED.name());
        event.setRrule(rrule);
        event.setCreator(userId);
        event.setCreateTime(now);
        event.setUpdater(userId);
        event.setUpdateTime(now);
        calEventMapper.insert(event);

        insertAttendee(event, userId, CalendarAttendeeRole.ORGANIZER, CalendarAttendeeResponse.ACCEPTED, now);
        for (Long attendeeId : attendeeIds) {
            insertAttendee(event, attendeeId, CalendarAttendeeRole.ATTENDEE, CalendarAttendeeResponse.NEEDS_ACTION, now);
        }

        calendarBotNotifyService.notifyInvite(event, attendeeIds);
        calendarBotNotifyService.pushRemindIfDue(event, userId);
        return event.getId();
    }

    @Override
    @Transactional
    public void update(CalEventUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        validateTime(request.getStartTime(), request.getEndTime());
        CalEventDO event = requireOrganizerEvent(request.getId(), userId);
        CalCalendarDO calendar = calCalendarService.requireOwnedCalendar(request.getCalendarId(), userId);

        CalEventRecurrenceSupport.EditScope scope = recurrenceSupport.parseEditScope(request.getEditScope(), event, request.getInstanceStart(), CalEventRecurrenceSupport.EditScope.ALL);
        String rrule = CalEventRecurrenceSupport.normalizeRrule(request.getRrule());
        RecurrenceExpander.validate(rrule);

        if (scope == CalEventRecurrenceSupport.EditScope.THIS) {
            recurrenceSupport.upsertException(event.getId(), request.getInstanceStart(), CalEventRecurrenceSupport.NOT_CANCELLED,
                    normalizeTitle(request.getTitle()), request.getStartTime(), request.getEndTime(),
                    userId, tenantId);
            notifyUpdateBestEffort(event, userId);
            return;
        }

        if (scope == CalEventRecurrenceSupport.EditScope.THIS_AND_FUTURE) {
            recurrenceSupport.truncateSeriesUntil(event, request.getInstanceStart(), userId);
            CalEventDO newMaster = createMasterFromRequest(request, event, calendar, rrule, userId, tenantId);
            copyAttendees(event.getId(), newMaster, userId);
            calendarBotNotifyService.notifyUpdate(newMaster, loadAttendeeUserIds(newMaster.getId()));
            calendarBotNotifyService.pushRemindIfDue(newMaster, userId);
            return;
        }

        Set<Long> previousAttendees = loadAttendeeUserIds(event.getId());
        Set<Long> nextAttendees = normalizeAttendeeIds(tenantId, userId, request.getAttendeeUserIds());

        event.setCalendarId(calendar.getId());
        event.setTitle(normalizeTitle(request.getTitle()));
        event.setDescription(blankToNull(request.getDescription()));
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setAllDay(Boolean.TRUE.equals(request.getAllDay()) ? 1 : 0);
        event.setRemindBeforeMinutes(request.getRemindBeforeMinutes());
        event.setAllDayRemindTime(blankToNull(request.getAllDayRemindTime()));
        event.setRrule(rrule);
        event.setUpdater(userId);
        event.setUpdateTime(OffsetDateTime.now());
        calEventMapper.updateById(event);

        replaceAttendees(event, nextAttendees, userId);

        Set<Long> newlyInvited = new LinkedHashSet<>(nextAttendees);
        newlyInvited.removeAll(previousAttendees);
        newlyInvited.remove(userId);
        if (!newlyInvited.isEmpty()) {
            calendarBotNotifyService.notifyInvite(event, newlyInvited);
        }
        Set<Long> stillAttending = new LinkedHashSet<>(nextAttendees);
        stillAttending.retainAll(previousAttendees);
        stillAttending.remove(userId);
        if (!stillAttending.isEmpty()) {
            calendarBotNotifyService.notifyUpdate(event, stillAttending);
        }
        calendarBotNotifyService.pushRemindIfDue(event, userId);
    }

    @Override
    @Transactional
    public void delete(Long id, String editScope, OffsetDateTime instanceStart) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        CalEventDO event = requireOrganizerEvent(id, userId);
        Set<Long> attendees = loadAttendeeUserIds(id);

        CalEventRecurrenceSupport.EditScope scope = recurrenceSupport.parseEditScope(editScope, event, instanceStart, CalEventRecurrenceSupport.EditScope.THIS);

        if (scope == CalEventRecurrenceSupport.EditScope.THIS) {
            recurrenceSupport.upsertException(event.getId(), instanceStart, CalEventRecurrenceSupport.CANCELLED, null, null, null, userId, tenantId);
            calendarBotNotifyService.notifyCancel(event, attendees);
            return;
        }

        if (scope == CalEventRecurrenceSupport.EditScope.THIS_AND_FUTURE) {
            recurrenceSupport.truncateSeriesUntil(event, instanceStart, userId);
            calendarBotNotifyService.notifyCancel(event, attendees);
            return;
        }

        event.setStatus(CalendarEventStatus.CANCELLED.name());
        event.setUpdater(userId);
        event.setUpdateTime(OffsetDateTime.now());
        calEventMapper.updateById(event);
        calEventMapper.deleteById(id);
        calendarBotNotifyService.notifyCancel(event, attendees);
    }

    @Override
    @Transactional
    public void reschedule(CalEventRescheduleReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        validateTime(request.getStartTime(), request.getEndTime());
        CalEventDO event = requireOrganizerEvent(request.getId(), userId);

        CalEventRecurrenceSupport.EditScope scope = recurrenceSupport.parseEditScope(request.getEditScope(), event, request.getInstanceStart(), CalEventRecurrenceSupport.EditScope.THIS);

        if (scope == CalEventRecurrenceSupport.EditScope.THIS && StringUtils.hasText(event.getRrule())) {
            recurrenceSupport.upsertException(event.getId(), request.getInstanceStart(), CalEventRecurrenceSupport.NOT_CANCELLED, null,
                    request.getStartTime(), request.getEndTime(), userId, tenantId);
            notifyUpdateBestEffort(event, userId);
            return;
        }

        CalEventUpdateReqVO update = buildUpdateFromEvent(event);
        update.setStartTime(request.getStartTime());
        update.setEndTime(request.getEndTime());
        update.setEditScope(CalEventRecurrenceSupport.EditScope.ALL.name());
        update.setInstanceStart(null);
        update(update);
    }

    @Override
    public void respond(CalEventRespondReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        String response = request.getResponse().trim().toUpperCase();
        if (!CalendarAttendeeResponse.ACCEPTED.name().equals(response)
                && !CalendarAttendeeResponse.DECLINED.name().equals(response)) {
            throw new ServiceException(ErrorCodeConstants.ATTENDEE_INVALID);
        }
        CalEventDO event = requireVisibleEvent(request.getId(), userId);
        if (Objects.equals(event.getOrganizerId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.EVENT_FORBIDDEN);
        }
        CalAttendeeDO attendee = calAttendeeMapper.selectOne(
                Wrappers.<CalAttendeeDO>lambdaQuery()
                        .eq(CalAttendeeDO::getEventId, event.getId())
                        .eq(CalAttendeeDO::getUserId, userId)
                        .eq(CalAttendeeDO::getRole, CalendarAttendeeRole.ATTENDEE.name())
                        .last("LIMIT 1"));
        if (attendee == null) {
            throw new ServiceException(ErrorCodeConstants.EVENT_FORBIDDEN);
        }
        attendee.setResponse(response);
        attendee.setUpdater(userId);
        attendee.setUpdateTime(OffsetDateTime.now());
        calAttendeeMapper.updateById(attendee);
    }

    private CalEventDO createMasterFromRequest(CalEventUpdateReqVO request, CalEventDO source,
                                               CalCalendarDO calendar, String rrule, Long userId, Long tenantId) {
        OffsetDateTime now = OffsetDateTime.now();
        CalEventDO event = new CalEventDO();
        event.setTenantId(tenantId);
        event.setCalendarId(calendar.getId());
        event.setTitle(normalizeTitle(request.getTitle()));
        event.setDescription(blankToNull(request.getDescription()));
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setAllDay(Boolean.TRUE.equals(request.getAllDay()) ? 1 : 0);
        event.setOrganizerId(source.getOrganizerId());
        event.setRemindBeforeMinutes(request.getRemindBeforeMinutes());
        event.setAllDayRemindTime(blankToNull(request.getAllDayRemindTime()));
        event.setStatus(CalendarEventStatus.CONFIRMED.name());
        event.setRrule(rrule);
        event.setCreator(userId);
        event.setCreateTime(now);
        event.setUpdater(userId);
        event.setUpdateTime(now);
        calEventMapper.insert(event);
        return event;
    }

    private void copyAttendees(Long sourceEventId, CalEventDO targetEvent, Long userId) {
        List<CalAttendeeDO> existing = calAttendeeMapper.selectList(
                Wrappers.<CalAttendeeDO>lambdaQuery().eq(CalAttendeeDO::getEventId, sourceEventId));
        OffsetDateTime now = OffsetDateTime.now();
        for (CalAttendeeDO row : existing) {
            CalAttendeeDO copy = new CalAttendeeDO();
            copy.setTenantId(targetEvent.getTenantId());
            copy.setEventId(targetEvent.getId());
            copy.setUserId(row.getUserId());
            copy.setRole(row.getRole());
            copy.setResponse(row.getResponse());
            copy.setCreator(userId);
            copy.setCreateTime(now);
            copy.setUpdater(userId);
            copy.setUpdateTime(now);
            calAttendeeMapper.insert(copy);
        }
    }

    private CalEventUpdateReqVO buildUpdateFromEvent(CalEventDO event) {
        CalEventUpdateReqVO update = new CalEventUpdateReqVO();
        update.setId(event.getId());
        update.setCalendarId(event.getCalendarId());
        update.setTitle(event.getTitle());
        update.setDescription(event.getDescription());
        update.setStartTime(event.getStartTime());
        update.setEndTime(event.getEndTime());
        update.setAllDay(event.getAllDay() != null && event.getAllDay() != 0);
        update.setRemindBeforeMinutes(event.getRemindBeforeMinutes());
        update.setAllDayRemindTime(event.getAllDayRemindTime());
        update.setRrule(event.getRrule());
        List<CalAttendeeDO> attendees = calAttendeeMapper.selectList(
                Wrappers.<CalAttendeeDO>lambdaQuery()
                        .eq(CalAttendeeDO::getEventId, event.getId())
                        .eq(CalAttendeeDO::getRole, CalendarAttendeeRole.ATTENDEE.name()));
        update.setAttendeeUserIds(attendees.stream().map(CalAttendeeDO::getUserId).toList());
        return update;
    }

    private void replaceAttendees(CalEventDO event, Set<Long> nextAttendees, Long userId) {
        List<CalAttendeeDO> existing = calAttendeeMapper.selectList(
                Wrappers.<CalAttendeeDO>lambdaQuery().eq(CalAttendeeDO::getEventId, event.getId()));
        for (CalAttendeeDO row : existing) {
            if (CalendarAttendeeRole.ATTENDEE.name().equals(row.getRole())) {
                calAttendeeMapper.deleteById(row.getId());
            }
        }
        OffsetDateTime now = OffsetDateTime.now();
        Map<Long, String> prevResponse = existing.stream()
                .filter(a -> CalendarAttendeeRole.ATTENDEE.name().equals(a.getRole()))
                .collect(Collectors.toMap(CalAttendeeDO::getUserId, CalAttendeeDO::getResponse, (a, b) -> a));
        for (Long attendeeId : nextAttendees) {
            String response = prevResponse.getOrDefault(attendeeId, CalendarAttendeeResponse.NEEDS_ACTION.name());
            insertAttendee(event, attendeeId, CalendarAttendeeRole.ATTENDEE,
                    CalendarAttendeeResponse.valueOf(response), now);
        }
    }

    private void notifyUpdateBestEffort(CalEventDO event, Long userId) {
        Set<Long> attendees = loadAttendeeUserIds(event.getId());
        attendees.remove(userId);
        if (!attendees.isEmpty()) {
            calendarBotNotifyService.notifyUpdate(event, attendees);
        }
    }

    private Set<Long> ownedCalendarIds(Long userId) {
        return calCalendarMapper.selectList(
                        Wrappers.<CalCalendarDO>lambdaQuery().eq(CalCalendarDO::getOwnerUserId, userId))
                .stream()
                .map(CalCalendarDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> normalizeAttendeeIds(Long tenantId, Long organizerId, List<Long> raw) {
        Set<Long> requested = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(raw)) {
            for (Long id : raw) {
                if (id != null && !Objects.equals(id, organizerId)) {
                    requested.add(id);
                }
            }
        }
        if (requested.isEmpty()) {
            return requested;
        }
        Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, requested);
        if (active.size() != requested.size()) {
            throw new ServiceException(ErrorCodeConstants.ATTENDEE_INVALID);
        }
        return requested;
    }

    private void insertAttendee(CalEventDO event, Long userId, CalendarAttendeeRole role,
                                CalendarAttendeeResponse response, OffsetDateTime now) {
        CalAttendeeDO row = new CalAttendeeDO();
        row.setTenantId(event.getTenantId());
        row.setEventId(event.getId());
        row.setUserId(userId);
        row.setRole(role.name());
        row.setResponse(response.name());
        row.setCreator(event.getOrganizerId());
        row.setCreateTime(now);
        row.setUpdater(event.getOrganizerId());
        row.setUpdateTime(now);
        calAttendeeMapper.insert(row);
    }

    private Set<Long> loadAttendeeUserIds(Long eventId) {
        return calAttendeeMapper.selectList(
                        Wrappers.<CalAttendeeDO>lambdaQuery().eq(CalAttendeeDO::getEventId, eventId))
                .stream()
                .map(CalAttendeeDO::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, CalCalendarDO> loadCalendars(List<CalEventDO> events) {
        Set<Long> ids = events.stream().map(CalEventDO::getCalendarId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return calCalendarMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CalCalendarDO::getId, c -> c, (a, b) -> a, HashMap::new));
    }

    private Map<Long, List<CalAttendeeDO>> loadAttendees(List<CalEventDO> events) {
        Set<Long> ids = events.stream().map(CalEventDO::getId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<CalAttendeeDO> rows = calAttendeeMapper.selectList(
                Wrappers.<CalAttendeeDO>lambdaQuery().in(CalAttendeeDO::getEventId, ids));
        Map<Long, List<CalAttendeeDO>> map = new HashMap<>();
        for (CalAttendeeDO row : rows) {
            map.computeIfAbsent(row.getEventId(), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private CalEventDO requireOrganizerEvent(Long id, Long userId) {
        CalEventDO event = calEventMapper.selectById(id);
        if (event == null || CalendarEventStatus.CANCELLED.name().equals(event.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.EVENT_NOT_FOUND);
        }
        if (!Objects.equals(event.getOrganizerId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.EVENT_FORBIDDEN);
        }
        return event;
    }

    private CalEventDO requireVisibleEvent(Long id, Long userId) {
        CalEventDO event = calEventMapper.selectById(id);
        if (event == null || CalendarEventStatus.CANCELLED.name().equals(event.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.EVENT_NOT_FOUND);
        }
        if (Objects.equals(event.getOrganizerId(), userId)) {
            return event;
        }
        Set<Long> owned = ownedCalendarIds(userId);
        if (owned.contains(event.getCalendarId())) {
            return event;
        }
        Set<Long> sharedRead = calCalendarShareService.sharedReadCalendarIds(userId);
        if (sharedRead.contains(event.getCalendarId())) {
            return event;
        }
        Long count = calAttendeeMapper.selectCount(
                Wrappers.<CalAttendeeDO>lambdaQuery()
                        .eq(CalAttendeeDO::getEventId, id)
                        .eq(CalAttendeeDO::getUserId, userId));
        if (count == null || count == 0) {
            throw new ServiceException(ErrorCodeConstants.EVENT_FORBIDDEN);
        }
        return event;
    }

    private CalEventRespVO toResp(CalEventDO master, CalEventRecurrenceSupport.ExpandedInstance instance, Long viewerId,
                                  Set<Long> ownedCalendarIds, Set<Long> sharedReadCalendarIds,
                                  Map<Long, CalCalendarDO> calendarMap,
                                  Map<Long, List<CalAttendeeDO>> attendeesByEvent) {
        CalCalendarDO calendar = calendarMap.get(master.getCalendarId());
        CalEventExceptionDO ex = instance.exception();

        CalEventRespVO vo = new CalEventRespVO();
        vo.setId(master.getId());
        vo.setCalendarId(master.getCalendarId());
        vo.setCalendarColor(calendar != null ? calendar.getColor() : "#3B82F6");
        vo.setCalendarName(calendar != null ? calendar.getName() : "");
        vo.setTitle(ex != null && StringUtils.hasText(ex.getOverrideTitle())
                ? ex.getOverrideTitle() : master.getTitle());
        vo.setDescription(master.getDescription());
        vo.setStartTime(ex != null && ex.getOverrideStart() != null ? ex.getOverrideStart() : instance.instanceStart());
        if (ex != null && ex.getOverrideEnd() != null) {
            vo.setEndTime(ex.getOverrideEnd());
        } else if (ex != null && ex.getOverrideStart() != null && ex.getOverrideEnd() == null) {
            long durationSeconds = java.time.temporal.ChronoUnit.SECONDS.between(master.getStartTime(), master.getEndTime());
            vo.setEndTime(ex.getOverrideStart().plusSeconds(durationSeconds));
        } else {
            vo.setEndTime(instance.instanceEnd());
        }
        vo.setAllDay(master.getAllDay() != null && master.getAllDay() != 0);
        vo.setOrganizerId(master.getOrganizerId());
        vo.setRemindBeforeMinutes(master.getRemindBeforeMinutes());
        vo.setAllDayRemindTime(master.getAllDayRemindTime());
        vo.setStatus(master.getStatus());
        boolean owned = ownedCalendarIds.contains(master.getCalendarId());
        boolean sharedRead = sharedReadCalendarIds.contains(master.getCalendarId());
        vo.setInvitedOnly(!owned && !sharedRead);
        vo.setViewerRole(Objects.equals(master.getOrganizerId(), viewerId)
                ? CalendarAttendeeRole.ORGANIZER.name()
                : CalendarAttendeeRole.ATTENDEE.name());
        vo.setRrule(master.getRrule());
        vo.setMasterEventId(master.getId());
        vo.setInstanceStart(instance.instanceStart());
        vo.setRecurring(StringUtils.hasText(master.getRrule()));
        vo.setIsException(ex != null && (Objects.equals(ex.getCancelled(), CalEventRecurrenceSupport.CANCELLED)
                || StringUtils.hasText(ex.getOverrideTitle())
                || ex.getOverrideStart() != null
                || ex.getOverrideEnd() != null));

        List<CalAttendeeDO> attendees = attendeesByEvent.getOrDefault(master.getId(), List.of());
        List<CalAttendeeRespVO> attendeeVos = new ArrayList<>(attendees.size());
        for (CalAttendeeDO a : attendees) {
            CalAttendeeRespVO item = new CalAttendeeRespVO();
            item.setUserId(a.getUserId());
            item.setRole(a.getRole());
            item.setResponse(a.getResponse());
            item.setNickname(resolveNickname(a.getUserId()));
            attendeeVos.add(item);
        }
        vo.setAttendees(attendeeVos);
        return vo;
    }

    private String resolveNickname(Long userId) {
        try {
            UserBasicDTO basic = userApi.getUserBasic(userId);
            if (basic != null && StringUtils.hasText(basic.getNickname())) {
                return basic.getNickname();
            }
            if (basic != null && StringUtils.hasText(basic.getUsername())) {
                return basic.getUsername();
            }
        } catch (Exception ex) {
            log.warn("Resolve nickname failed (best-effort): userId={}", userId, ex);
        }
        return String.valueOf(userId);
    }

    private static void validateTime(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ServiceException(ErrorCodeConstants.EVENT_TIME_INVALID);
        }
    }

    private static String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "(无主题)";
        }
        return title.trim();
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
