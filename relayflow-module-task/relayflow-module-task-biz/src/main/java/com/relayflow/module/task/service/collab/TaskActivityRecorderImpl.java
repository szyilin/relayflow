package com.relayflow.module.task.service.collab;

import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.task.dal.dataobject.TaskActivityDO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TaskActivityRecorderImpl implements TaskActivityRecorder {

    private final TaskActivityMapper taskActivityMapper;

    @Override
    public void record(TaskItemDO task, Long actorId, String type, String summary) {
        if (task == null || task.getId() == null || actorId == null || !StringUtils.hasText(type)) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        TaskActivityDO row = new TaskActivityDO();
        row.setTenantId(task.getTenantId());
        row.setTaskId(task.getId());
        row.setTaskTitle(StringUtils.hasText(task.getTitle()) ? task.getTitle() : "任务");
        row.setActorId(actorId);
        row.setType(type);
        row.setSummary(StringUtils.hasText(summary) ? summary : type);
        row.setCreator(actorId);
        row.setCreateTime(now);
        row.setUpdater(actorId);
        row.setUpdateTime(now);
        taskActivityMapper.insert(row);
    }

    public void recordCurrentUser(TaskItemDO task, String type, String summary) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        record(task, userId, type, summary);
    }
}
