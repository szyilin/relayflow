package com.relayflow.module.task.service.item;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mysql.TaskItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskItemStatus;
import com.relayflow.module.task.service.notify.TaskDueNotifyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private TaskItemServiceImpl taskItemService;

    @BeforeEach
    void setUpMocks() {
        taskItemService = new TaskItemServiceImpl(taskItemMapper, taskDueNotifyService);
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

        Long id = taskItemService.createTask(USER_ID, TENANT_ID, request);

        assertEquals(TASK_ID, id);
        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).insert(captor.capture());
        verify(taskDueNotifyService).pushIfDueSoon(any(TaskItemDO.class));
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

        PageResult<TaskItemRespVO> result = taskItemService.pageMyTasks(USER_ID, request);

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
    void toggleDone_throwsForbiddenWhenNotAssignee() {
        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setAssigneeId(999L);
        row.setStatus(TaskItemStatus.TODO);
        when(taskItemMapper.selectById(TASK_ID)).thenReturn(row);

        TaskItemToggleDoneReqVO request = new TaskItemToggleDoneReqVO();
        request.setId(TASK_ID);
        request.setDone(true);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> taskItemService.toggleDone(USER_ID, request));
        assertEquals(ErrorCodeConstants.TASK_FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void deleteTask_throwsNotFoundWhenMissing() {
        when(taskItemMapper.selectById(TASK_ID)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> taskItemService.deleteTask(USER_ID, TASK_ID));
        assertEquals(ErrorCodeConstants.TASK_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void updateTask_updatesTitleForOwnedTask() {
        TaskItemDO row = new TaskItemDO();
        row.setId(TASK_ID);
        row.setAssigneeId(USER_ID);
        row.setTitle("旧标题");
        when(taskItemMapper.selectById(TASK_ID)).thenReturn(row);

        TaskItemUpdateReqVO request = new TaskItemUpdateReqVO();
        request.setId(TASK_ID);
        request.setTitle("新标题");

        taskItemService.updateTask(USER_ID, request);

        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).updateById(captor.capture());
        assertEquals("新标题", captor.getValue().getTitle());
    }
}
