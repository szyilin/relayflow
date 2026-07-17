package com.relayflow.module.calendar.service.event;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.calendar.controller.app.vo.CalEventCreateReqVO;
import com.relayflow.module.calendar.dal.dataobject.CalAttendeeDO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.dataobject.CalEventDO;
import com.relayflow.module.calendar.dal.mapper.CalAttendeeMapper;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalEventMapper;
import com.relayflow.module.calendar.enums.CalendarAttendeeRole;
import com.relayflow.module.calendar.enums.CalendarType;
import com.relayflow.module.calendar.service.calendar.CalCalendarService;
import com.relayflow.module.calendar.service.notify.CalendarBotNotifyService;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalEventServiceImplTest {

    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;
    private static final long CAL_ID = 10L;
    private static final long PEER_ID = 200L;

    @Mock
    private CalEventMapper calEventMapper;
    @Mock
    private CalAttendeeMapper calAttendeeMapper;
    @Mock
    private CalCalendarMapper calCalendarMapper;
    @Mock
    private CalCalendarService calCalendarService;
    @Mock
    private TenantMemberApi tenantMemberApi;
    @Mock
    private UserApi userApi;
    @Mock
    private CalendarBotNotifyService calendarBotNotifyService;

    private CalEventServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CalEventServiceImpl(
                calEventMapper, calAttendeeMapper, calCalendarMapper,
                calCalendarService, tenantMemberApi, userApi, calendarBotNotifyService);
        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_insertsOrganizerAndInvitees() {
        CalCalendarDO calendar = new CalCalendarDO();
        calendar.setId(CAL_ID);
        calendar.setOwnerUserId(USER_ID);
        calendar.setType(CalendarType.OWNED.name());
        calendar.setName("工作");
        calendar.setColor("#EC4899");
        when(calCalendarService.requireOwnedCalendar(CAL_ID, USER_ID)).thenReturn(calendar);
        when(tenantMemberApi.filterActiveMemberUserIds(eq(TENANT_ID), anyCollection()))
                .thenReturn(Set.of(PEER_ID));
        when(calEventMapper.insert(any(CalEventDO.class))).thenAnswer(invocation -> {
            CalEventDO row = invocation.getArgument(0);
            row.setId(5001L);
            return 1;
        });

        CalEventCreateReqVO req = new CalEventCreateReqVO();
        req.setCalendarId(CAL_ID);
        req.setTitle("周会");
        req.setStartTime(OffsetDateTime.parse("2026-07-17T18:30:00+08:00"));
        req.setEndTime(OffsetDateTime.parse("2026-07-17T19:00:00+08:00"));
        req.setAllDay(false);
        req.setRemindBeforeMinutes(5);
        req.setAttendeeUserIds(List.of(PEER_ID));

        Long id = service.create(req);
        assertEquals(5001L, id);

        ArgumentCaptor<CalAttendeeDO> attendeeCaptor = ArgumentCaptor.forClass(CalAttendeeDO.class);
        verify(calAttendeeMapper, org.mockito.Mockito.times(2)).insert(attendeeCaptor.capture());
        List<CalAttendeeDO> attendees = attendeeCaptor.getAllValues();
        assertEquals(CalendarAttendeeRole.ORGANIZER.name(), attendees.get(0).getRole());
        assertEquals(CalendarAttendeeRole.ATTENDEE.name(), attendees.get(1).getRole());
        assertEquals(PEER_ID, attendees.get(1).getUserId());
        verify(calendarBotNotifyService).notifyInvite(any(CalEventDO.class), eq(Set.of(PEER_ID)));
    }
}
