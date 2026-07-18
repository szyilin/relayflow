package com.relayflow.module.task.service.listitem;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.dataobject.TaskListItemDO;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.dal.mapper.TaskListItemMapper;
import com.relayflow.module.task.service.listgroup.TaskListGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskListItemServiceTest {

    private static final long TASK_ID = 2001L;
    private static final long LIST_A = 501L;
    private static final long LIST_B = 502L;
    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;
    private static final long DEFAULT_GROUP_A = 601L;
    private static final long DEFAULT_GROUP_B = 602L;

    @Mock
    private TaskListItemMapper taskListItemMapper;
    @Mock
    private TaskItemMapper taskItemMapper;
    @Mock
    private TaskListGroupService taskListGroupService;

    private TaskListItemService service;

    @BeforeEach
    void setUp() {
        service = new TaskListItemService(taskListItemMapper, taskItemMapper, taskListGroupService);
    }

    @Test
    void replaceMemberships_insertsAndSyncsProjection() {
        TaskItemDO task = task();
        when(taskListItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(taskListGroupService.ensureDefaultGroupId(eq(LIST_A), eq(TENANT_ID), eq(USER_ID)))
                .thenReturn(DEFAULT_GROUP_A);
        when(taskListGroupService.ensureDefaultGroupId(eq(LIST_B), eq(TENANT_ID), eq(USER_ID)))
                .thenReturn(DEFAULT_GROUP_B);

        service.replaceMemberships(task, USER_ID, TENANT_ID, List.of(LIST_A, LIST_B));

        ArgumentCaptor<TaskListItemDO> itemCaptor = ArgumentCaptor.forClass(TaskListItemDO.class);
        verify(taskListItemMapper, org.mockito.Mockito.times(2)).insert(itemCaptor.capture());
        assertEquals(DEFAULT_GROUP_A, itemCaptor.getAllValues().get(0).getGroupId());
        assertEquals(DEFAULT_GROUP_B, itemCaptor.getAllValues().get(1).getGroupId());
        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).updateById(captor.capture());
        assertEquals(LIST_A, captor.getValue().getListId());
    }

    @Test
    void replaceMemberships_emptyClearsProjection() {
        TaskItemDO task = task();
        task.setListId(LIST_A);
        TaskListItemDO existing = new TaskListItemDO();
        existing.setId(1L);
        existing.setTaskId(TASK_ID);
        existing.setListId(LIST_A);
        when(taskListItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        service.replaceMemberships(task, USER_ID, TENANT_ID, List.of());

        verify(taskListItemMapper).deleteById(1L);
        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).updateById(captor.capture());
        assertNull(captor.getValue().getListId());
    }

    @Test
    void ensureMembership_skipsInsertWhenPresent() {
        TaskItemDO task = task();
        TaskListItemDO existing = new TaskListItemDO();
        existing.setTaskId(TASK_ID);
        existing.setListId(LIST_A);
        when(taskListItemMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(taskListItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        service.ensureMembership(task, LIST_A, USER_ID, TENANT_ID);

        verify(taskListItemMapper, never()).insert(any(TaskListItemDO.class));
    }

    private static TaskItemDO task() {
        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setTenantId(TENANT_ID);
        return row;
    }
}
