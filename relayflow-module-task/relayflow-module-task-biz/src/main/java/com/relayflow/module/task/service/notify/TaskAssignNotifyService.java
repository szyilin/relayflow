package com.relayflow.module.task.service.notify;

import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Best-effort assign notify via {@code task-bot} interactive card ({@code open_url}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskAssignNotifyService {

    private static final String TASK_BOT_CODE = "task-bot";
    private static final String DEDUPE_PREFIX = "TASK_ASSIGN:";

    private final ImBotApi imBotApi;

    public void notifyAssignee(TaskItemDO task, Long assigneeId) {
        if (task == null || task.getId() == null || assigneeId == null || task.getTenantId() == null) {
            return;
        }
        ImBotSendTarget target = new ImBotSendTarget();
        target.setScope(ImBotSendTarget.SCOPE_SINGLE);
        target.setTenantId(task.getTenantId());
        target.setUserId(assigneeId);

        ImBotSendCommand command = new ImBotSendCommand();
        command.setBotCode(TASK_BOT_CODE);
        command.setCard(TaskBotCardFactory.assigned(task));
        command.setDedupeKey(DEDUPE_PREFIX + task.getId() + ":" + assigneeId);
        command.setRoute("/app/tasks?taskId=" + task.getId());
        command.setEntityType("task");
        command.setEntityId(String.valueOf(task.getId()));
        command.setTarget(target);

        try {
            imBotApi.send(command);
        } catch (Exception ex) {
            log.warn("Task assign bot message failed: taskId={}, assigneeId={}, botCode={}",
                    task.getId(), assigneeId, TASK_BOT_CODE, ex);
        }
    }
}
