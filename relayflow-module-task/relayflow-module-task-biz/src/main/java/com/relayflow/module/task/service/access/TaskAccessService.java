package com.relayflow.module.task.service.access;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.task.dal.dataobject.TaskFollowerDO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskFollowerMapper;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Task visibility: assignee / creator / follower may read;
 * assignee / creator may edit core fields and assign.
 */
@Service
@RequiredArgsConstructor
public class TaskAccessService {

    private final TaskItemMapper taskItemMapper;
    private final TaskFollowerMapper taskFollowerMapper;

    public TaskItemDO requireTask(Long id) {
        TaskItemDO row = taskItemMapper.selectById(id);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_NOT_FOUND);
        }
        return row;
    }

    public TaskItemDO requireAccessible(Long id, Long userId) {
        TaskItemDO row = requireTask(id);
        if (!canAccess(row, userId)) {
            throw new ServiceException(ErrorCodeConstants.TASK_FORBIDDEN);
        }
        return row;
    }

    public TaskItemDO requireEditable(Long id, Long userId) {
        TaskItemDO row = requireTask(id);
        if (!canEdit(row, userId)) {
            throw new ServiceException(ErrorCodeConstants.TASK_FORBIDDEN);
        }
        return row;
    }

    public boolean canAccess(TaskItemDO row, Long userId) {
        return canEdit(row, userId) || isFollower(row.getId(), userId);
    }

    public boolean canEdit(TaskItemDO row, Long userId) {
        return Objects.equals(row.getAssigneeId(), userId)
                || Objects.equals(row.getCreatorId(), userId);
    }

    public boolean isFollower(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        return taskFollowerMapper.selectCount(
                Wrappers.<TaskFollowerDO>lambdaQuery()
                        .eq(TaskFollowerDO::getTaskId, taskId)
                        .eq(TaskFollowerDO::getUserId, userId)) > 0;
    }
}
