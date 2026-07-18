package com.relayflow.module.task.service.access;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.task.dal.dataobject.TaskFollowerDO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.dataobject.TaskListItemDO;
import com.relayflow.module.task.dal.dataobject.TaskListMemberDO;
import com.relayflow.module.task.dal.mapper.TaskFollowerMapper;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.dal.mapper.TaskListItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskListRole;
import com.relayflow.module.task.service.assignee.TaskAssigneeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Task visibility: assignee (set) / creator / follower / list member may read;
 * assignee (set) / creator / list OWNER|EDITOR may edit core fields.
 */
@Service
@RequiredArgsConstructor
public class TaskAccessService {

    private final TaskItemMapper taskItemMapper;
    private final TaskFollowerMapper taskFollowerMapper;
    private final TaskListItemMapper taskListItemMapper;
    private final TaskListAccessService taskListAccessService;
    private final TaskAssigneeService taskAssigneeService;

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
        return canEdit(row, userId)
                || isFollower(row.getId(), userId)
                || isMemberOfAnyTaskList(row, userId);
    }

    public boolean canEdit(TaskItemDO row, Long userId) {
        if (Objects.equals(row.getCreatorId(), userId)
                || taskAssigneeService.isAssignee(row.getId(), userId)) {
            return true;
        }
        return canMutateViaAnyTaskList(row, userId);
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

    private List<Long> resolveListIds(TaskItemDO row) {
        List<Long> fromJunction = taskListItemMapper.selectList(
                        Wrappers.<TaskListItemDO>lambdaQuery()
                                .eq(TaskListItemDO::getTaskId, row.getId()))
                .stream()
                .map(TaskListItemDO::getListId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!fromJunction.isEmpty()) {
            return fromJunction;
        }
        if (row.getListId() != null) {
            return List.of(row.getListId());
        }
        return List.of();
    }

    private boolean isMemberOfAnyTaskList(TaskItemDO row, Long userId) {
        for (Long listId : resolveListIds(row)) {
            if (taskListAccessService.findMembership(listId, userId) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean canMutateViaAnyTaskList(TaskItemDO row, Long userId) {
        for (Long listId : resolveListIds(row)) {
            TaskListMemberDO member = taskListAccessService.findMembership(listId, userId);
            if (member != null && TaskListRole.canMutateTasks(member.getRole())) {
                return true;
            }
        }
        return false;
    }
}
