package com.relayflow.module.task.service.viewconfig;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.task.controller.app.vo.TaskViewConfigSaveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskViewConfigVO;
import com.relayflow.module.task.dal.dataobject.TaskListMemberDO;
import com.relayflow.module.task.dal.dataobject.TaskViewConfigDO;
import com.relayflow.module.task.dal.mapper.TaskViewConfigMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskListRole;
import com.relayflow.module.task.enums.TaskViewContextType;
import com.relayflow.module.task.service.access.TaskListAccessService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskViewConfigServiceImplTest {

    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;
    private static final long LIST_ID = 5001L;

    @Mock
    private TaskViewConfigMapper taskViewConfigMapper;
    @Mock
    private TaskListAccessService taskListAccessService;

    private TaskViewConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskViewConfigServiceImpl(
                taskViewConfigMapper, taskListAccessService, new ObjectMapper());
        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void get_returnsDefaultWhenMissing() {
        when(taskViewConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        TaskViewConfigVO vo = service.get(TaskViewContextType.MINE, null);

        assertEquals("LIST", vo.getDisplayMode());
        assertNull(vo.getGroupBy());
        verify(taskViewConfigMapper).selectOne(any(Wrapper.class));
    }

    @Test
    void save_personalInsertsRow() {
        when(taskViewConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        TaskViewConfigSaveReqVO req = new TaskViewConfigSaveReqVO();
        req.setContextType(TaskViewContextType.MINE);
        TaskViewConfigVO config = new TaskViewConfigVO();
        config.setDisplayMode("BOARD");
        config.setSort(Map.of("key", "dueTime", "order", "ASC"));
        config.setFilters(List.of());
        config.setVisibleFieldKeys(List.of("dueTime"));
        req.setConfig(config);

        service.save(req);

        ArgumentCaptor<TaskViewConfigDO> captor = ArgumentCaptor.forClass(TaskViewConfigDO.class);
        verify(taskViewConfigMapper).insert(captor.capture());
        assertEquals(TaskViewContextType.MINE, captor.getValue().getContextType());
        assertEquals(USER_ID, captor.getValue().getOwnerUserId());
        assertNull(captor.getValue().getContextId());
    }

    @Test
    void save_listViewerForbidden() {
        TaskListMemberDO member = new TaskListMemberDO();
        member.setRole(TaskListRole.VIEWER);
        when(taskListAccessService.requireMembership(LIST_ID, USER_ID)).thenReturn(member);

        TaskViewConfigSaveReqVO req = new TaskViewConfigSaveReqVO();
        req.setContextType(TaskViewContextType.LIST);
        req.setContextId(LIST_ID);
        TaskViewConfigVO config = TaskViewConfigServiceImpl.defaultConfig(TaskViewContextType.LIST);
        req.setConfig(config);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.save(req));
        assertEquals(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN.getCode(), ex.getCode());
        verify(taskViewConfigMapper, never()).insert(any(TaskViewConfigDO.class));
        verify(taskViewConfigMapper, never()).updateById(any(TaskViewConfigDO.class));
    }

    @Test
    void save_listEditorAllowed() {
        TaskListMemberDO member = new TaskListMemberDO();
        member.setRole(TaskListRole.EDITOR);
        when(taskListAccessService.requireMembership(eq(LIST_ID), eq(USER_ID))).thenReturn(member);
        when(taskViewConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        TaskViewConfigSaveReqVO req = new TaskViewConfigSaveReqVO();
        req.setContextType(TaskViewContextType.LIST);
        req.setContextId(LIST_ID);
        req.setConfig(TaskViewConfigServiceImpl.defaultConfig(TaskViewContextType.LIST));

        service.save(req);

        ArgumentCaptor<TaskViewConfigDO> captor = ArgumentCaptor.forClass(TaskViewConfigDO.class);
        verify(taskViewConfigMapper).insert(captor.capture());
        assertEquals(LIST_ID, captor.getValue().getContextId());
        assertNull(captor.getValue().getOwnerUserId());
    }
}
