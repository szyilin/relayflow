package com.relayflow.module.task.service.notify;

import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
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
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDueNotifyServiceTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 100L;
    private static final long TASK_ID = 2001L;

    @Mock
    private TaskProperties taskProperties;

    @Mock
    private ImBotApi imBotApi;

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

    @Test
    void pushIfDueSoon_sendsTaskBotSingleWithDedupeAndDeepLink() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        OffsetDateTime dueTime = OffsetDateTime.of(2026, 7, 16, 18, 30, 0, 0, ZoneOffset.ofHours(8));
        TaskItemDO task = todoTask(dueTime);

        taskDueNotifyService.pushIfDueSoon(task);

        ArgumentCaptor<ImBotSendCommand> captor = ArgumentCaptor.forClass(ImBotSendCommand.class);
        verify(imBotApi).send(captor.capture());
        ImBotSendCommand command = captor.getValue();
        assertEquals("task-bot", command.getBotCode());
        assertEquals("「整理周报」将在 2026-07-16 18:30 到期", command.getText());
        assertEquals("TASK_DUE:2001", command.getDedupeKey());
        assertEquals("/app/tasks?taskId=2001", command.getRoute());
        assertEquals("task", command.getEntityType());
        assertEquals("2001", command.getEntityId());
        assertEquals(ImBotSendTarget.SCOPE_SINGLE, command.getTarget().getScope());
        assertEquals(TENANT_ID, command.getTarget().getTenantId());
        assertEquals(USER_ID, command.getTarget().getUserId());
    }

    @Test
    void pushIfDueSoon_skipsSendOutsideWindow() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        TaskItemDO task = todoTask(OffsetDateTime.now().plusDays(3));

        taskDueNotifyService.pushIfDueSoon(task);

        verify(imBotApi, never()).send(any());
    }

    @Test
    void pushIfDueSoon_swallowsSendFailure() {
        when(taskProperties.getDueRemindWindow()).thenReturn(Duration.ofHours(24));
        doThrow(new RuntimeException("bot not enabled")).when(imBotApi).send(any());

        taskDueNotifyService.pushIfDueSoon(todoTask(OffsetDateTime.now().plusHours(1)));

        verify(imBotApi).send(any());
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
