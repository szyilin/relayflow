package com.relayflow.module.task.service.notify;

import com.relayflow.module.infra.api.notify.NotifyInboxApi;
import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.enums.InfraNotifyType;
import com.relayflow.module.task.config.TaskProperties;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.enums.TaskItemStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDueNotifyServiceTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 100L;
    private static final long TASK_ID = 2001L;

    @Mock
    private NotifyInboxApi notifyInboxApi;
    @Mock
    private TaskProperties taskProperties;

    @InjectMocks
    private TaskDueNotifyService taskDueNotifyService;

    @Test
    void pushIfDueSoon_pushesWhenDueWithinWindow() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        TaskItemDO task = todoTask(OffsetDateTime.now().plusHours(2));

        taskDueNotifyService.pushIfDueSoon(task);

        ArgumentCaptor<NotifyItemCommand> captor = ArgumentCaptor.forClass(NotifyItemCommand.class);
        verify(notifyInboxApi).push(captor.capture());
        NotifyItemCommand command = captor.getValue();
        assertEquals(InfraNotifyType.TASK_DUE, command.getType());
        assertEquals("task:" + TASK_ID, command.getDedupeKey());
        assertEquals("/app/tasks?taskId=" + TASK_ID, command.getPayload().get("route"));
    }

    @Test
    void pushIfDueSoon_skipsWhenDueBeyondWindow() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        TaskItemDO task = todoTask(OffsetDateTime.now().plusDays(3));

        taskDueNotifyService.pushIfDueSoon(task);

        verify(notifyInboxApi, never()).push(any());
    }

    @Test
    void compensateMissingDueReminders_pushesWhenUnreadMissing() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        TaskItemDO task = todoTask(OffsetDateTime.now().plusHours(1));
        when(notifyInboxApi.hasUnreadDedupe(TENANT_ID, USER_ID, InfraNotifyType.TASK_DUE, "task:" + TASK_ID))
                .thenReturn(false);

        taskDueNotifyService.compensateMissingDueReminders(List.of(task));

        verify(notifyInboxApi).push(any());
    }

    @Test
    void compensateMissingDueReminders_skipsWhenUnreadExists() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        TaskItemDO task = todoTask(OffsetDateTime.now().plusHours(1));
        when(notifyInboxApi.hasUnreadDedupe(TENANT_ID, USER_ID, InfraNotifyType.TASK_DUE, "task:" + TASK_ID))
                .thenReturn(true);

        taskDueNotifyService.compensateMissingDueReminders(List.of(task));

        verify(notifyInboxApi, never()).push(any());
    }

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
