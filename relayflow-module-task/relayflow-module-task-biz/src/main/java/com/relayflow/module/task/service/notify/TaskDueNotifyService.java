package com.relayflow.module.task.service.notify;

import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import com.relayflow.module.task.config.TaskProperties;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.enums.TaskItemStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Delivers task due reminders via {@code task-bot} + {@link ImBotApi}
 * ({@code im-bot-task-due-migrate}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskDueNotifyService {

    private static final String TASK_BOT_CODE = "task-bot";
    private static final String TASK_DUE_DEDUPE_PREFIX = "TASK_DUE:";
    private static final DateTimeFormatter DUE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TaskProperties taskProperties;
    private final ImBotApi imBotApi;

    public void pushIfDueSoon(TaskItemDO task) {
        if (!shouldRemind(task)) {
            return;
        }
        sendDueReminder(task);
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

    private void sendDueReminder(TaskItemDO task) {
        ImBotSendTarget target = new ImBotSendTarget();
        target.setScope(ImBotSendTarget.SCOPE_SINGLE);
        target.setTenantId(task.getTenantId());
        target.setUserId(task.getAssigneeId());

        ImBotSendCommand command = new ImBotSendCommand();
        command.setBotCode(TASK_BOT_CODE);
        command.setText(buildReminderText(task));
        command.setDedupeKey(TASK_DUE_DEDUPE_PREFIX + task.getId());
        command.setRoute("/app/tasks?taskId=" + task.getId());
        command.setEntityType("task");
        command.setEntityId(String.valueOf(task.getId()));
        command.setTarget(target);

        try {
            imBotApi.send(command);
        } catch (Exception ex) {
            // Best-effort reach: task CRUD / list must succeed even if Bot delivery fails.
            log.warn("Task due bot message failed: taskId={}, assigneeId={}, botCode={}",
                    task.getId(), task.getAssigneeId(), TASK_BOT_CODE, ex);
        }
    }

    private static String buildReminderText(TaskItemDO task) {
        String title = StringUtils.hasText(task.getTitle()) ? task.getTitle() : "未命名任务";
        String due = task.getDueTime().format(DUE_TIME_FORMAT);
        return "「" + title + "」将在 " + due + " 到期";
    }
}
