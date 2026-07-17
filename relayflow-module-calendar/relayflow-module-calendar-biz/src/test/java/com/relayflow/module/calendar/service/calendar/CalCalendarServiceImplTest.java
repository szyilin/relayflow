package com.relayflow.module.calendar.service.calendar;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarRespVO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalEventMapper;
import com.relayflow.module.calendar.enums.CalendarType;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalCalendarServiceImplTest {

    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;

    @Mock
    private CalCalendarMapper calCalendarMapper;
    @Mock
    private CalEventMapper calEventMapper;

    private CalCalendarServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CalCalendarServiceImpl(calCalendarMapper, calEventMapper);
        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMine_ensuresPrimaryWhenMissing() {
        when(calCalendarMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(calCalendarMapper.insert(any(CalCalendarDO.class))).thenAnswer(invocation -> {
            CalCalendarDO row = invocation.getArgument(0);
            row.setId(9001L);
            return 1;
        });
        when(calCalendarMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            CalCalendarDO primary = new CalCalendarDO();
            primary.setId(9001L);
            primary.setName("我的日历");
            primary.setColor("#3B82F6");
            primary.setType(CalendarType.PRIMARY.name());
            primary.setOwnerUserId(USER_ID);
            return List.of(primary);
        });

        List<CalCalendarRespVO> list = service.listMine();

        assertEquals(1, list.size());
        assertEquals(CalendarType.PRIMARY.name(), list.get(0).getType());
        ArgumentCaptor<CalCalendarDO> captor = ArgumentCaptor.forClass(CalCalendarDO.class);
        verify(calCalendarMapper).insert(captor.capture());
        assertEquals(CalendarType.PRIMARY.name(), captor.getValue().getType());
    }

    @Test
    void create_ownedCalendar() {
        when(calCalendarMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            CalCalendarDO primary = new CalCalendarDO();
            primary.setId(1L);
            primary.setType(CalendarType.PRIMARY.name());
            return primary;
        });
        when(calCalendarMapper.insert(any(CalCalendarDO.class))).thenAnswer(invocation -> {
            CalCalendarDO row = invocation.getArgument(0);
            row.setId(9002L);
            return 1;
        });

        CalCalendarCreateReqVO req = new CalCalendarCreateReqVO();
        req.setName("工作");
        req.setColor("#EC4899");

        Long id = service.create(req);
        assertEquals(9002L, id);
        ArgumentCaptor<CalCalendarDO> captor = ArgumentCaptor.forClass(CalCalendarDO.class);
        verify(calCalendarMapper).insert(captor.capture());
        assertEquals(CalendarType.OWNED.name(), captor.getValue().getType());
    }

    @Test
    void delete_primaryForbidden() {
        CalCalendarDO primary = new CalCalendarDO();
        primary.setId(1L);
        primary.setOwnerUserId(USER_ID);
        primary.setType(CalendarType.PRIMARY.name());
        when(calCalendarMapper.selectById(1L)).thenReturn(primary);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.delete(1L));
        assertEquals(ErrorCodeConstants.PRIMARY_CALENDAR_DELETE_FORBIDDEN.getCode(), ex.getCode());
    }
}
