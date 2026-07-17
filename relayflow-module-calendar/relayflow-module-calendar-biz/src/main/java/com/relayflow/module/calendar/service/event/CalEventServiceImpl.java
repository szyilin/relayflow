package com.relayflow.module.calendar.service.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.app.vo.CalAttendeeRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRespondReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventUpdateReqVO;
import com.relayflow.module.calendar.dal.dataobject.CalAttendeeDO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.dataobject.CalEventDO;
import com.relayflow.module.calendar.dal.mapper.CalAttendeeMapper;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalEventMapper;
import com.relayflow.module.calendar.enums.CalendarAttendeeResponse;
import com.relayflow.module.calendar.enums.CalendarAttendeeRole;
import com.relayflow.module.calendar.enums.CalendarEventStatus;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import com.relayflow.module.calendar.service.calendar.CalCalendarService;
import com.relayflow.module.calendar.service.notify.CalendarBotNotifyService;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalEventServiceImpl implements CalEventService {

    private final CalEventMapper calEventMapper;
    private final CalAttendeeMapper calAttendeeMapper;
    private final CalCalendarMapper calCalendarMapper;
    private final CalCalendarService calCalendarService;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;
    private final CalendarBotNotifyService calendarBotNotifyService;

    @Override
    public List<CalEventRespVO> list(OffsetDateTime from, OffsetDateTime to, Set<Long> calendarIdsFilter) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ServiceException(ErrorCodeConstants.EVENT_TIME_INVALID);
        }
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        calCalendarService.ensurePrimary(tenantId, userId);

        Set<Long> ownedIds = ownedCalendarIds(userId);
        Set<Long> ownedFilter = ownedIds;
        if (calendarIdsFilter != null && !calendarIdsFilter.isEmpty()) {
            ownedFilter = ownedIds.stream().filter(calendarIdsFilter::contains).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        Map<Long, CalEventDO> byId = new LinkedHashMap<>();

        if (!ownedFilter.isEmpty()) {
            for (CalEventDO event : selectOverlapping(ownedFilter, from, to)) {
                byId.put(event.getId(), event);
            }
        }

        List<CalAttendeeDO> myAttend = calAttendeeMapper.selectList(
                Wrappers.<CalAttendeeDO>lambdaQuery().eq(CalAttendeeDO::getUserId, userId));
        Set<Long> attendEventIds = myAttend.stream()
                .map(CalAttendeeDO::getEventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        attendEventIds.removeAll(byId.keySet());
        if (!attendEventIds.isEmpty()) {
            List<CalEventDO> invited = calEventMapper.selectList(
                    Wrappers.<CalEventDO>lambdaQuery()
                            .in(CalEventDO::getId, attendEventIds)
                            .eq(CalEventDO::getStatus, CalendarEventStatus.CONFIRMED.name())
                            .lt(CalEventDO::getStartTime, to)
                            .gt(CalEventDO::getEndTime, from));
            for (CalEventDO event : invited) {
                byId.putIfAbsent(event.getId(), event);
            }
        }

        List<CalEventDO> events = new ArrayList<>(byId.values());
        events.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        Map<Long, CalCalendarDO> calendarMap = loadCalendars(events);
        Map<Long, List<CalAttendeeDO>> attendeesByEvent = loadAttendees(events);
        List<CalEventRespVO> result = new ArrayList<>(events.size());
        for (CalEventDO event : events) {
            CalEventRespVO vo = toResp(event, userId, ownedIds, calendarMap, attendeesByEvent);
            result.add(vo);
            // remind compensate for self
            calendarBotNotifyService.pushRemindIfDue(event, userId);
        }
        return result;
    }

    @Override
    public CalEventRespVO get(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalEventDO event = requireVisibleEvent(id, userId);
        Set<Long> ownedIds = ownedCalendarIds(userId);
        Map<Long, CalCalendarDO> calendarMap = loadCalendars(List.of(event));
        Map<Long, List<CalAttendeeDO>> attendeesByEvent = loadAttendees(List.of(event));
        return toResp(event, userId, ownedIds, calendarMap, attendeesByEvent);
    }

    @Override
    @Transactional
    public Long create(CalEventCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        validateTime(request.getStartTime(), request.getEndTime());
        CalCalendarDO calendar = calCalendarService.requireOwnedCalendar(request.getCalendarId(), userId);

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
        event.setUpdater(userId);
        event.setUpdateTime(OffsetDateTime.now());
        calEventMapper.updateById(event);

        // Replace ATTENDEE rows; keep ORGANIZER.
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
    public void delete(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalEventDO event = requireOrganizerEvent(id, userId);
        Set<Long> attendees = loadAttendeeUserIds(id);
        event.setStatus(CalendarEventStatus.CANCELLED.name());
        event.setUpdater(userId);
        event.setUpdateTime(OffsetDateTime.now());
        calEventMapper.updateById(event);
        calEventMapper.deleteById(id);
        calendarBotNotifyService.notifyCancel(event, attendees);
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

    private List<CalEventDO> selectOverlapping(Set<Long> calendarIds, OffsetDateTime from, OffsetDateTime to) {
        return calEventMapper.selectList(
                Wrappers.<CalEventDO>lambdaQuery()
                        .in(CalEventDO::getCalendarId, calendarIds)
                        .eq(CalEventDO::getStatus, CalendarEventStatus.CONFIRMED.name())
                        .lt(CalEventDO::getStartTime, to)
                        .gt(CalEventDO::getEndTime, from));
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
        Long count = calAttendeeMapper.selectCount(
                Wrappers.<CalAttendeeDO>lambdaQuery()
                        .eq(CalAttendeeDO::getEventId, id)
                        .eq(CalAttendeeDO::getUserId, userId));
        if (count == null || count == 0) {
            throw new ServiceException(ErrorCodeConstants.EVENT_FORBIDDEN);
        }
        return event;
    }

    private CalEventRespVO toResp(CalEventDO event, Long viewerId, Set<Long> ownedCalendarIds,
                                  Map<Long, CalCalendarDO> calendarMap,
                                  Map<Long, List<CalAttendeeDO>> attendeesByEvent) {
        CalCalendarDO calendar = calendarMap.get(event.getCalendarId());
        CalEventRespVO vo = new CalEventRespVO();
        vo.setId(event.getId());
        vo.setCalendarId(event.getCalendarId());
        vo.setCalendarColor(calendar != null ? calendar.getColor() : "#3B82F6");
        vo.setCalendarName(calendar != null ? calendar.getName() : "");
        vo.setTitle(event.getTitle());
        vo.setDescription(event.getDescription());
        vo.setStartTime(event.getStartTime());
        vo.setEndTime(event.getEndTime());
        vo.setAllDay(event.getAllDay() != null && event.getAllDay() != 0);
        vo.setOrganizerId(event.getOrganizerId());
        vo.setRemindBeforeMinutes(event.getRemindBeforeMinutes());
        vo.setAllDayRemindTime(event.getAllDayRemindTime());
        vo.setStatus(event.getStatus());
        boolean invitedOnly = !ownedCalendarIds.contains(event.getCalendarId());
        vo.setInvitedOnly(invitedOnly);
        vo.setViewerRole(Objects.equals(event.getOrganizerId(), viewerId)
                ? CalendarAttendeeRole.ORGANIZER.name()
                : CalendarAttendeeRole.ATTENDEE.name());

        List<CalAttendeeDO> attendees = attendeesByEvent.getOrDefault(event.getId(), List.of());
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
        } catch (Exception ignored) {
            // best-effort display
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
