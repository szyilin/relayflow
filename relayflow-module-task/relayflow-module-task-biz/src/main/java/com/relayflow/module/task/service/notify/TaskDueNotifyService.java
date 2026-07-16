package com.relayflow.module.task.service.notify;

import com.relayflow.module.task.config.TaskProperties;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.enums.TaskItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Temporary no-op after Notify Inbox hard-cut ({@code im-bot-notify-foundation}).
 * Due reminders will be re-wired via {@code ImBotApi} + {@code task-bot} in
 * {@code im-bot-task-due-migrate}.
 */
@Service
@RequiredArgsConstructor
public class TaskDueNotifyService {

    private final TaskProperties taskProperties;

    public void pushIfDueSoon(TaskItemDO task) {
        // Notify write path removed; migration slice will call ImBotApi.
        if (!shouldRemind(task)) {
            return;
        }
    }

    public void compensateMissingDueReminders(List<TaskItemDO> tasks) {
        for (TaskItemDO task : tasks) {
            pushIfDueSoon(task);
        }
    }

    boolean shouldRemind(TaskItemDO task) {
        if (task == null || task.getId() == null || task.getAssigneeId() == null || task.getTenantId() == null) {
            return false;
        }
        if (!TaskItemStatus.TODO.equals(task.getStatus()) || task.getDueTime() == null) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowEnd = now.plus(taskProperties.getDueRemindWindow());
        OffsetDateTime dueTime = task.getDueTime();
        return !dueTime.isBefore(now) && !dueTime.isAfter(windowEnd);
    }
}
