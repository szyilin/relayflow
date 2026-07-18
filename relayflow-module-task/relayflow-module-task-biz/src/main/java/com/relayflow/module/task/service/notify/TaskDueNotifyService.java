package com.relayflow.module.task.service.notify;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import com.relayflow.module.task.config.TaskProperties;
import com.relayflow.module.task.dal.dataobject.TaskItemAssigneeDO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskItemAssigneeMapper;
import com.relayflow.module.task.enums.TaskItemStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

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
    private final TaskItemAssigneeMapper taskItemAssigneeMapper;

    public void pushIfDueSoon(TaskItemDO task) {
        if (!shouldRemind(task)) {
            return;
        }
        List<Long> assigneeIds = listAssigneeUserIds(task.getId());
        if (assigneeIds.isEmpty() && task.getAssigneeId() != null) {
            assigneeIds = List.of(task.getAssigneeId());
        }
        for (Long assigneeId : assigneeIds) {
            sendDueReminder(task, assigneeId);
        }
    }

    public void compensateMissingDueReminders(List<TaskItemDO> tasks) {
        for (TaskItemDO task : tasks) {
            pushIfDueSoon(task);
        }
    }

    boolean shouldRemind(TaskItemDO task) {
        if (task == null || task.getId() == null || task.getTenantId() == null) {
            return false;
        }
        if (!TaskItemStatus.isOpen(task.getStatus()) || task.getDueTime() == null) {
            return false;
        }
        List<Long> assigneeIds = listAssigneeUserIds(task.getId());
        if (assigneeIds.isEmpty() && task.getAssigneeId() == null) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowEnd = now.plus(taskProperties.getDueRemindWindow());
        OffsetDateTime dueTime = task.getDueTime();
        return !dueTime.isBefore(now) && !dueTime.isAfter(windowEnd);
    }

    private List<Long> listAssigneeUserIds(Long taskId) {
        return taskItemAssigneeMapper.selectList(
                        Wrappers.<TaskItemAssigneeDO>lambdaQuery()
                                .eq(TaskItemAssigneeDO::getTaskId, taskId)
                                .orderByAsc(TaskItemAssigneeDO::getUserId))
                .stream()
                .map(TaskItemAssigneeDO::getUserId)
                .filter(Objects::nonNull)
                .toList();
    }

    private void sendDueReminder(TaskItemDO task, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        ImBotSendTarget target = new ImBotSendTarget();
        target.setScope(ImBotSendTarget.SCOPE_SINGLE);
        target.setTenantId(task.getTenantId());
        target.setUserId(assigneeId);

        ImBotSendCommand command = new ImBotSendCommand();
        command.setBotCode(TASK_BOT_CODE);
        command.setText(buildReminderText(task));
        command.setDedupeKey(TASK_DUE_DEDUPE_PREFIX + task.getId() + ":" + assigneeId);
        command.setRoute("/app/tasks?taskId=" + task.getId());
        command.setEntityType("task");
        command.setEntityId(String.valueOf(task.getId()));
        command.setTarget(target);

        try {
            imBotApi.send(command);
        } catch (Exception ex) {
            log.warn("Task due bot message failed: taskId={}, assigneeId={}, botCode={}",
                    task.getId(), assigneeId, TASK_BOT_CODE, ex);
        }
    }

    private static String buildReminderText(TaskItemDO task) {
        String title = StringUtils.hasText(task.getTitle()) ? task.getTitle() : "未命名任务";
        String due = task.getDueTime().format(DUE_TIME_FORMAT);
        return "「" + title + "」将在 " + due + " 到期";
    }
}
