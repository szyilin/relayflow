package com.relayflow.module.task.service.notify;

import com.relayflow.module.infra.api.notify.NotifyInboxApi;
import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.enums.InfraNotifyType;
import com.relayflow.module.task.config.TaskProperties;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.enums.TaskItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskDueNotifyService {

    private final NotifyInboxApi notifyInboxApi;
    private final TaskProperties taskProperties;

    public void pushIfDueSoon(TaskItemDO task) {
        if (!shouldRemind(task)) {
            return;
        }
        notifyInboxApi.push(buildCommand(task));
    }

    public void compensateMissingDueReminders(List<TaskItemDO> tasks) {
        for (TaskItemDO task : tasks) {
            if (!shouldRemind(task)) {
                continue;
            }
            String dedupeKey = dedupeKey(task.getId());
            if (notifyInboxApi.hasUnreadDedupe(task.getTenantId(), task.getAssigneeId(),
                    InfraNotifyType.TASK_DUE, dedupeKey)) {
                continue;
            }
            notifyInboxApi.push(buildCommand(task));
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

    private NotifyItemCommand buildCommand(TaskItemDO task) {
        NotifyItemCommand command = new NotifyItemCommand();
        command.setTenantId(task.getTenantId());
        command.setUserId(task.getAssigneeId());
        command.setType(InfraNotifyType.TASK_DUE);
        command.setTitle("任务即将到期");
        command.setBody("「" + task.getTitle() + "」将在 " + task.getDueTime() + " 到期");
        command.setDedupeKey(dedupeKey(task.getId()));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("route", "/app/tasks?taskId=" + task.getId());
        payload.put("entityType", "task");
        payload.put("entityId", String.valueOf(task.getId()));
        payload.put("dueTime", task.getDueTime().toString());
        command.setPayload(payload);
        return command;
    }

    private static String dedupeKey(Long taskId) {
        return "task:" + taskId;
    }
}
