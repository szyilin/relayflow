package com.relayflow.module.task.service.notify;

import com.relayflow.module.task.config.TaskProperties;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.enums.TaskItemStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDueNotifyServiceTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 100L;
    private static final long TASK_ID = 2001L;

    @Mock
    private TaskProperties taskProperties;

    @InjectMocks
    private TaskDueNotifyService taskDueNotifyService;

    @Test
    void shouldRemind_returnsFalseForDoneTask() {
        TaskItemDO task = todoTask(OffsetDateTime.now().plusHours(1));
        task.setStatus(TaskItemStatus.DONE);
        assertFalse(taskDueNotifyService.shouldRemind(task));
    }

    @Test
    void shouldRemind_returnsTrueInsideWindow() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        TaskItemDO task = todoTask(OffsetDateTime.now().plusHours(1));
        assertTrue(taskDueNotifyService.shouldRemind(task));
    }

    @Test
    void shouldRemind_returnsFalseBeyondWindow() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        TaskItemDO task = todoTask(OffsetDateTime.now().plusDays(3));
        assertFalse(taskDueNotifyService.shouldRemind(task));
    }

    private TaskItemDO todoTask(OffsetDateTime dueTime) {
        TaskItemDO task = new TaskItemDO();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setAssigneeId(USER_ID);
        task.setTitle("整理周报");
        task.setStatus(TaskItemStatus.TODO);
        task.setDueTime(dueTime);
        return task;
    }
}
