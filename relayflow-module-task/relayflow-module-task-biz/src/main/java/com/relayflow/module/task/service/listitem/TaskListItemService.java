package com.relayflow.module.task.service.listitem;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.dataobject.TaskListItemDO;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.dal.mapper.TaskListItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskListItemService {

    private final TaskListItemMapper taskListItemMapper;
    private final TaskItemMapper taskItemMapper;

    public List<Long> listListIds(Long taskId) {
        if (taskId == null) {
            return List.of();
        }
        return taskListItemMapper.selectList(
                        Wrappers.<TaskListItemDO>lambdaQuery()
                                .eq(TaskListItemDO::getTaskId, taskId)
                                .orderByAsc(TaskListItemDO::getRank)
                                .orderByAsc(TaskListItemDO::getId))
                .stream()
                .map(TaskListItemDO::getListId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public boolean isMemberOfList(Long taskId, Long listId) {
        if (taskId == null || listId == null) {
            return false;
        }
        return taskListItemMapper.selectCount(
                Wrappers.<TaskListItemDO>lambdaQuery()
                        .eq(TaskListItemDO::getTaskId, taskId)
                        .eq(TaskListItemDO::getListId, listId)) > 0;
    }

    /**
     * Full replace of list memberships for a root task. Does not delete the task.
     * Caller must authorize. Syncs compat {@code task_item.list_id} to first id.
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceMemberships(
            TaskItemDO task,
            Long operatorUserId,
            Long tenantId,
            Collection<Long> listIds) {
        LinkedHashSet<Long> next = new LinkedHashSet<>();
        if (listIds != null) {
            for (Long id : listIds) {
                if (id != null) {
                    next.add(id);
                }
            }
        }
        Set<Long> previous = new HashSet<>(listListIds(task.getId()));
        if (previous.equals(next)) {
            syncProjection(task, next);
            return;
        }

        List<TaskListItemDO> existing = taskListItemMapper.selectList(
                Wrappers.<TaskListItemDO>lambdaQuery()
                        .eq(TaskListItemDO::getTaskId, task.getId()));
        OffsetDateTime now = OffsetDateTime.now();
        for (TaskListItemDO row : existing) {
            if (!next.contains(row.getListId())) {
                taskListItemMapper.deleteById(row.getId());
            }
        }
        Set<Long> remaining = new HashSet<>(listListIds(task.getId()));
        int rank = 0;
        for (Long listId : next) {
            if (remaining.contains(listId)) {
                rank++;
                continue;
            }
            TaskListItemDO row = new TaskListItemDO();
            row.setTenantId(tenantId);
            row.setListId(listId);
            row.setTaskId(task.getId());
            row.setRank(rank++);
            row.setCreator(operatorUserId);
            row.setCreateTime(now);
            row.setUpdater(operatorUserId);
            row.setUpdateTime(now);
            taskListItemMapper.insert(row);
        }
        syncProjection(task, next);
    }

    /** Ensure single list membership (create-in-list). */
    @Transactional(rollbackFor = Exception.class)
    public void ensureMembership(
            TaskItemDO task,
            Long listId,
            Long operatorUserId,
            Long tenantId) {
        if (listId == null) {
            syncProjection(task, List.of());
            return;
        }
        if (isMemberOfList(task.getId(), listId)) {
            syncProjection(task, listListIds(task.getId()));
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        TaskListItemDO row = new TaskListItemDO();
        row.setTenantId(tenantId);
        row.setListId(listId);
        row.setTaskId(task.getId());
        row.setRank(0);
        row.setCreator(operatorUserId);
        row.setCreateTime(now);
        row.setUpdater(operatorUserId);
        row.setUpdateTime(now);
        taskListItemMapper.insert(row);
        List<Long> ids = new ArrayList<>(listListIds(task.getId()));
        syncProjection(task, ids);
    }

    /** Copy parent memberships onto a new subtask (V1). */
    @Transactional(rollbackFor = Exception.class)
    public void copyMembershipsFromParent(
            TaskItemDO child,
            Long parentTaskId,
            Long operatorUserId,
            Long tenantId) {
        List<Long> parentLists = listListIds(parentTaskId);
        if (parentLists.isEmpty()) {
            // Legacy parent may only have list_id column
            TaskItemDO parent = taskItemMapper.selectById(parentTaskId);
            if (parent != null && parent.getListId() != null) {
                parentLists = List.of(parent.getListId());
            }
        }
        replaceMemberships(child, operatorUserId, tenantId, parentLists);
    }

    private void syncProjection(TaskItemDO task, Collection<Long> listIds) {
        Long first = null;
        if (listIds != null) {
            for (Long id : listIds) {
                if (id != null) {
                    first = id;
                    break;
                }
            }
        }
        if (Objects.equals(task.getListId(), first)) {
            return;
        }
        task.setListId(first);
        taskItemMapper.updateById(task);
    }
}
