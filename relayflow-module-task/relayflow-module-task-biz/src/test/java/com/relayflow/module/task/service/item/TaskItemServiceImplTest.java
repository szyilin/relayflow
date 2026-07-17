package com.relayflow.module.task.service.item;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskSubtaskCreateReqVO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskItemStatus;
import com.relayflow.module.task.service.access.TaskAccessService;
import com.relayflow.module.task.service.collab.TaskActivityRecorder;
import com.relayflow.module.task.service.notify.TaskDueNotifyService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskItemServiceImplTest {

    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;
    private static final long TASK_ID = 2001L;

    @Mock
    private TaskItemMapper taskItemMapper;
    @Mock
    private TaskDueNotifyService taskDueNotifyService;
    @Mock
    private TaskAccessService taskAccessService;
    @Mock
    private TaskActivityRecorder taskActivityRecorder;

    private TaskItemServiceImpl taskItemService;

    @BeforeEach
    void setUpMocks() {
        taskItemService = new TaskItemServiceImpl(
                taskItemMapper, taskDueNotifyService, taskAccessService, taskActivityRecorder);
        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTask_assignsCurrentUserAsAssigneeAndCreator() {
        TaskItemCreateReqVO request = new TaskItemCreateReqVO();
        request.setTitle("整理周报");

        when(taskItemMapper.insert(any(TaskItemDO.class))).thenAnswer(invocation -> {
            TaskItemDO row = invocation.getArgument(0);
            row.setId(TASK_ID);
            return 1;
        });

        Long id = taskItemService.createTask(request);

        assertEquals(TASK_ID, id);
        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).insert(captor.capture());
        verify(taskDueNotifyService).pushIfDueSoon(any(TaskItemDO.class));
        verify(taskActivityRecorder).record(any(TaskItemDO.class), eq(USER_ID), any(), any());
        TaskItemDO saved = captor.getValue();
        assertEquals("整理周报", saved.getTitle());
        assertEquals(USER_ID, saved.getAssigneeId());
        assertEquals(USER_ID, saved.getCreatorId());
        assertEquals(TaskItemStatus.TODO, saved.getStatus());
        assertEquals(TENANT_ID, saved.getTenantId());
    }

    @Test
    void pageMyTasks_filtersByAssignee() {
        TaskItemPageReqVO request = new TaskItemPageReqVO();
        request.setPageNo(1);
        request.setPageSize(20);

        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setTitle("整理周报");
        row.setStatus(TaskItemStatus.TODO);
        row.setCreateTime(OffsetDateTime.now());

        Page<TaskItemDO> page = new Page<>(1, 20);
        page.setRecords(List.of(row));
        page.setTotal(1);
        when(taskItemMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        PageResult<TaskItemRespVO> result = taskItemService.pageMyTasks(request);

        assertEquals(1, result.getTotal());
        assertEquals(TASK_ID, result.getList().get(0).getId());
        verify(taskItemMapper).selectPage(any(Page.class), any(Wrapper.class));
        verify(taskDueNotifyService).compensateMissingDueReminders(page.getRecords());
    }

    @Test
    void searchMyTasks_filtersByAssigneeAndTitle() {
        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setTitle("整理周报");
        row.setStatus(TaskItemStatus.TODO);
        row.setCreateTime(OffsetDateTime.now());
        when(taskItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(row));

        List<TaskItemRespVO> result = taskItemService.searchMyTasks(USER_ID, "周报", 5);

        assertEquals(1, result.size());
        assertEquals(TASK_ID, result.get(0).getId());
        verify(taskItemMapper).selectList(any(Wrapper.class));
    }

    @Test
    void listDueRange_queriesTodoWithDueInWindow() {
        OffsetDateTime from = OffsetDateTime.parse("2026-07-17T00:00:00+08:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-18T00:00:00+08:00");
        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setTitle("整理周报");
        row.setStatus(TaskItemStatus.TODO);
        row.setDueTime(OffsetDateTime.parse("2026-07-17T18:00:00+08:00"));
        when(taskItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(row));

        List<TaskItemRespVO> result = taskItemService.listDueRange(USER_ID, from, to, 200);

        assertEquals(1, result.size());
        assertEquals(TASK_ID, result.get(0).getId());
        verify(taskItemMapper).selectList(any(Wrapper.class));
    }

    @Test
    void listDueRange_emptyWhenInvalidWindow() {
        OffsetDateTime from = OffsetDateTime.parse("2026-07-18T00:00:00+08:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-17T00:00:00+08:00");

        List<TaskItemRespVO> result = taskItemService.listDueRange(USER_ID, from, to, 200);

        assertEquals(0, result.size());
    }

    @Test
    void toggleDone_throwsForbiddenWhenNotAssignee() {
        when(taskAccessService.requireEditable(TASK_ID, USER_ID))
                .thenThrow(new ServiceException(ErrorCodeConstants.TASK_FORBIDDEN));

        TaskItemToggleDoneReqVO request = new TaskItemToggleDoneReqVO();
        request.setId(TASK_ID);
        request.setDone(true);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> taskItemService.toggleDone(request));
        assertEquals(ErrorCodeConstants.TASK_FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void deleteTask_throwsNotFoundWhenMissing() {
        when(taskAccessService.requireEditable(TASK_ID, USER_ID))
                .thenThrow(new ServiceException(ErrorCodeConstants.TASK_NOT_FOUND));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> taskItemService.deleteTask(TASK_ID));
        assertEquals(ErrorCodeConstants.TASK_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void updateTask_updatesTitleForOwnedTask() {
        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setAssigneeId(USER_ID);
        row.setTitle("旧标题");
        when(taskAccessService.requireEditable(TASK_ID, USER_ID)).thenReturn(row);

        TaskItemUpdateReqVO request = new TaskItemUpdateReqVO();
        request.setId(TASK_ID);
        request.setTitle("新标题");

        taskItemService.updateTask(request);

        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).updateById(captor.capture());
        assertEquals("新标题", captor.getValue().getTitle());
        verify(taskDueNotifyService).pushIfDueSoon(captor.getValue());
    }

    @Test
    void createSubtask_rejectsWhenParentIsAlreadySubtask() {
        TaskItemDO parent = new TaskItemDO();
        parent.setId(TASK_ID);
        parent.setAssigneeId(USER_ID);
        parent.setParentId(999L);
        when(taskAccessService.requireEditable(TASK_ID, USER_ID)).thenReturn(parent);

        TaskSubtaskCreateReqVO request = new TaskSubtaskCreateReqVO();
        request.setParentId(TASK_ID);
        request.setTitle("再下一层");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> taskItemService.createSubtask(request));
        assertEquals(ErrorCodeConstants.TASK_SUBTASK_DEPTH_EXCEEDED.getCode(), exception.getCode());
    }

    @Test
    void updateTask_rejectsWhenStartAfterDue() {
        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setAssigneeId(USER_ID);
        when(taskAccessService.requireEditable(TASK_ID, USER_ID)).thenReturn(row);

        TaskItemUpdateReqVO request = new TaskItemUpdateReqVO();
        request.setId(TASK_ID);
        request.setStartTime(OffsetDateTime.parse("2026-07-18T18:00:00+08:00"));
        request.setDueTime(OffsetDateTime.parse("2026-07-17T18:00:00+08:00"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> taskItemService.updateTask(request));
        assertEquals(ErrorCodeConstants.TASK_INVALID_TIME_RANGE.getCode(), exception.getCode());
    }
}
