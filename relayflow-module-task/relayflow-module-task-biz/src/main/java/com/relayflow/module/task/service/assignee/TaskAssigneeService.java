package com.relayflow.module.task.service.assignee;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.task.dal.dataobject.TaskItemAssigneeDO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskItemAssigneeMapper;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskActivityType;
import com.relayflow.module.task.service.collab.TaskActivityRecorder;
import com.relayflow.module.task.service.notify.TaskAssignNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskAssigneeService {

    private final TaskItemAssigneeMapper taskItemAssigneeMapper;
    private final TaskItemMapper taskItemMapper;
    private final TenantMemberApi tenantMemberApi;
    private final TaskActivityRecorder taskActivityRecorder;
    private final TaskAssignNotifyService taskAssignNotifyService;

    public boolean isAssignee(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        return taskItemAssigneeMapper.selectCount(
                Wrappers.<TaskItemAssigneeDO>lambdaQuery()
                        .eq(TaskItemAssigneeDO::getTaskId, taskId)
                        .eq(TaskItemAssigneeDO::getUserId, userId)) > 0;
    }

    public List<Long> listAssigneeIds(Long taskId) {
        if (taskId == null) {
            return List.of();
        }
        return taskItemAssigneeMapper.selectList(
                        Wrappers.<TaskItemAssigneeDO>lambdaQuery()
                                .eq(TaskItemAssigneeDO::getTaskId, taskId)
                                .orderByAsc(TaskItemAssigneeDO::getUserId))
                .stream()
                .map(TaskItemAssigneeDO::getUserId)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Long> listTaskIdsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return taskItemAssigneeMapper.selectList(
                        Wrappers.<TaskItemAssigneeDO>lambdaQuery()
                                .eq(TaskItemAssigneeDO::getUserId, userId)
                                .select(TaskItemAssigneeDO::getTaskId))
                .stream()
                .map(TaskItemAssigneeDO::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public Map<Long, List<Long>> mapAssigneeIdsByTaskIds(Collection<Long> taskIds) {
        Map<Long, List<Long>> map = new HashMap<>();
        if (taskIds == null || taskIds.isEmpty()) {
            return map;
        }
        List<TaskItemAssigneeDO> rows = taskItemAssigneeMapper.selectList(
                Wrappers.<TaskItemAssigneeDO>lambdaQuery()
                        .in(TaskItemAssigneeDO::getTaskId, taskIds)
                        .orderByAsc(TaskItemAssigneeDO::getUserId));
        for (TaskItemAssigneeDO row : rows) {
            map.computeIfAbsent(row.getTaskId(), k -> new ArrayList<>()).add(row.getUserId());
        }
        return map;
    }

    public void replaceAssignees(
            TaskItemDO task,
            Long operatorUserId,
            Long tenantId,
            Collection<Long> assigneeIds,
            boolean recordActivity
    ) {
        replaceAssignees(task, operatorUserId, tenantId, assigneeIds, recordActivity, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceAssignees(
            TaskItemDO task,
            Long operatorUserId,
            Long tenantId,
            Collection<Long> assigneeIds,
            boolean recordActivity,
            boolean notifyNew
    ) {
        LinkedHashSet<Long> next = new LinkedHashSet<>();
        if (assigneeIds != null) {
            for (Long id : assigneeIds) {
                if (id != null) {
                    next.add(id);
                }
            }
        }
        requireActiveMembers(tenantId, next);

        Set<Long> previous = new HashSet<>(listAssigneeIds(task.getId()));
        if (previous.equals(next)) {
            return;
        }

        for (Long oldId : previous) {
            if (!next.contains(oldId)) {
                taskItemAssigneeMapper.delete(
                        Wrappers.<TaskItemAssigneeDO>lambdaQuery()
                                .eq(TaskItemAssigneeDO::getTaskId, task.getId())
                                .eq(TaskItemAssigneeDO::getUserId, oldId));
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Long newId : next) {
            if (previous.contains(newId)) {
                continue;
            }
            TaskItemAssigneeDO row = new TaskItemAssigneeDO();
            row.setTenantId(tenantId);
            row.setTaskId(task.getId());
            row.setUserId(newId);
            row.setCreator(operatorUserId);
            row.setCreateTime(now);
            row.setUpdater(operatorUserId);
            row.setUpdateTime(now);
            taskItemAssigneeMapper.insert(row);
        }

        syncProjection(task, operatorUserId, next);

        if (recordActivity) {
            String summary = next.isEmpty()
                    ? "清除了负责人"
                    : "将负责人设为 " + next.stream().map(String::valueOf).collect(Collectors.joining(", "));
            taskActivityRecorder.record(task, operatorUserId, TaskActivityType.ASSIGNED, summary);
        }

        if (notifyNew) {
            for (Long added : next) {
                if (!previous.contains(added)) {
                    taskAssignNotifyService.notifyAssignee(task, added);
                }
            }
        }
    }

    private void syncProjection(TaskItemDO task, Long operatorUserId, Set<Long> next) {
        Long projected = next.stream().min(Long::compareTo).orElse(null);
        task.setAssigneeId(projected);
        if (next.contains(operatorUserId) || next.isEmpty()) {
            task.setAssignerId(null);
        } else {
            task.setAssignerId(operatorUserId);
        }
        task.setUpdater(operatorUserId);
        task.setUpdateTime(OffsetDateTime.now());
        taskItemMapper.updateById(task);
    }

    private void requireActiveMembers(Long tenantId, Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        List<Long> ids = List.copyOf(userIds);
        Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, ids);
        for (Long id : ids) {
            if (active == null || !active.contains(id)) {
                throw new ServiceException(ErrorCodeConstants.TASK_ASSIGNEE_NOT_MEMBER);
            }
        }
    }
}
