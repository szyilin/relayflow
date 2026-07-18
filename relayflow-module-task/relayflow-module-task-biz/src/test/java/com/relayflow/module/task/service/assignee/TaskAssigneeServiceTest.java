package com.relayflow.module.task.service.assignee;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.task.dal.dataobject.TaskItemAssigneeDO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskItemAssigneeMapper;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.service.collab.TaskActivityRecorder;
import com.relayflow.module.task.service.notify.TaskAssignNotifyService;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAssigneeServiceTest {

    private static final long USER_ID = 100L;
    private static final long OTHER_ID = 200L;
    private static final long TENANT_ID = 1L;
    private static final long TASK_ID = 2001L;

    @Mock
    private TaskItemAssigneeMapper taskItemAssigneeMapper;
    @Mock
    private TaskItemMapper taskItemMapper;
    @Mock
    private TenantMemberApi tenantMemberApi;
    @Mock
    private TaskActivityRecorder taskActivityRecorder;
    @Mock
    private TaskAssignNotifyService taskAssignNotifyService;

    private TaskAssigneeService service;

    @BeforeEach
    void setUp() {
        service = new TaskAssigneeService(
                taskItemAssigneeMapper,
                taskItemMapper,
                tenantMemberApi,
                taskActivityRecorder,
                taskAssignNotifyService);
        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void replaceAssignees_setsAssignerWhenOperatorNotInSet() {
        TaskItemDO task = task();
        when(taskItemAssigneeMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(tenantMemberApi.filterActiveMemberUserIds(eq(TENANT_ID), any()))
                .thenReturn(Set.of(OTHER_ID));

        service.replaceAssignees(task, USER_ID, TENANT_ID, List.of(OTHER_ID), false, false);

        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).updateById(captor.capture());
        assertEquals(OTHER_ID, captor.getValue().getAssigneeId());
        assertEquals(USER_ID, captor.getValue().getAssignerId());
        verify(taskItemAssigneeMapper).insert(any(TaskItemAssigneeDO.class));
    }

    @Test
    void replaceAssignees_clearsAssignerWhenOperatorRemainsAssignee() {
        TaskItemDO task = task();
        TaskItemAssigneeDO existing = new TaskItemAssigneeDO();
        existing.setTaskId(TASK_ID);
        existing.setUserId(OTHER_ID);
        when(taskItemAssigneeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));
        when(tenantMemberApi.filterActiveMemberUserIds(eq(TENANT_ID), any()))
                .thenReturn(Set.of(USER_ID, OTHER_ID));

        service.replaceAssignees(task, USER_ID, TENANT_ID, List.of(USER_ID, OTHER_ID), false, false);

        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).updateById(captor.capture());
        assertNull(captor.getValue().getAssignerId());
        verify(taskAssignNotifyService, never()).notifyAssignee(any(), any());
    }

    @Test
    void replaceAssignees_clearsAssignerWhenSetEmpty() {
        TaskItemDO task = task();
        TaskItemAssigneeDO existing = new TaskItemAssigneeDO();
        existing.setTaskId(TASK_ID);
        existing.setUserId(OTHER_ID);
        when(taskItemAssigneeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        service.replaceAssignees(task, USER_ID, TENANT_ID, List.of(), false, false);

        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).updateById(captor.capture());
        assertNull(captor.getValue().getAssigneeId());
        assertNull(captor.getValue().getAssignerId());
    }

    private TaskItemDO task() {
        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setTenantId(TENANT_ID);
        row.setTitle("t");
        return row;
    }
}
