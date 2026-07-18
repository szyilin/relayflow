package com.relayflow.module.task.service.listgroup;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.task.controller.app.vo.TaskListGroupCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupMembershipVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupMoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupUpdateReqVO;
import com.relayflow.module.task.dal.dataobject.TaskListGroupDO;
import com.relayflow.module.task.dal.dataobject.TaskListItemDO;
import com.relayflow.module.task.dal.mapper.TaskListGroupMapper;
import com.relayflow.module.task.dal.mapper.TaskListItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.service.access.TaskAccessService;
import com.relayflow.module.task.service.access.TaskListAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskListGroupServiceImpl implements TaskListGroupService {

    private static final String DEFAULT_NAME = "默认";

    private final TaskListGroupMapper taskListGroupMapper;
    private final TaskListItemMapper taskListItemMapper;
    private final TaskListAccessService taskListAccessService;
    private final TaskAccessService taskAccessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskListGroupListRespVO list(Long listId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        taskListAccessService.requireReadable(listId, userId);
        ensureDefaultGroup(listId, tenantId, userId);

        List<TaskListGroupDO> groups = taskListGroupMapper.selectList(
                Wrappers.<TaskListGroupDO>lambdaQuery()
                        .eq(TaskListGroupDO::getListId, listId)
                        .orderByAsc(TaskListGroupDO::getRank)
                        .orderByAsc(TaskListGroupDO::getId));

        List<TaskListItemDO> items = taskListItemMapper.selectList(
                Wrappers.<TaskListItemDO>lambdaQuery()
                        .eq(TaskListItemDO::getListId, listId)
                        .orderByAsc(TaskListItemDO::getRank)
                        .orderByAsc(TaskListItemDO::getId));

        Long defaultId = groups.stream()
                .filter(g -> Objects.equals(g.getIsDefault(), 1))
                .map(TaskListGroupDO::getId)
                .findFirst()
                .orElse(null);

        TaskListGroupListRespVO resp = new TaskListGroupListRespVO();
        resp.setGroups(groups.stream().map(this::toGroupVo).toList());
        resp.setMemberships(items.stream().map(item -> toMembershipVo(item, defaultId)).toList());
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskListGroupRespVO create(TaskListGroupCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        taskListAccessService.requireCanMutateTasks(request.getListId(), userId);
        ensureDefaultGroup(request.getListId(), tenantId, userId);

        String name = request.getName() == null ? "" : request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_GROUP_NAME_EMPTY);
        }

        int nextRank = taskListGroupMapper.selectList(
                        Wrappers.<TaskListGroupDO>lambdaQuery()
                                .eq(TaskListGroupDO::getListId, request.getListId()))
                .stream()
                .map(TaskListGroupDO::getRank)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        OffsetDateTime now = OffsetDateTime.now();
        TaskListGroupDO row = new TaskListGroupDO();
        row.setTenantId(tenantId);
        row.setListId(request.getListId());
        row.setName(name);
        row.setRank(nextRank);
        row.setIsDefault(0);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        taskListGroupMapper.insert(row);
        return toGroupVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TaskListGroupUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskListGroupDO row = requireGroup(request.getId());
        taskListAccessService.requireCanMutateTasks(row.getListId(), userId);

        boolean changed = false;
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (!StringUtils.hasText(name)) {
                throw new ServiceException(ErrorCodeConstants.TASK_LIST_GROUP_NAME_EMPTY);
            }
            row.setName(name);
            changed = true;
        }
        if (request.getRank() != null) {
            row.setRank(request.getRank());
            changed = true;
        }
        if (!changed) {
            return;
        }
        row.setUpdater(userId);
        row.setUpdateTime(OffsetDateTime.now());
        taskListGroupMapper.updateById(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        TaskListGroupDO row = requireGroup(id);
        taskListAccessService.requireCanMutateTasks(row.getListId(), userId);
        if (Objects.equals(row.getIsDefault(), 1)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_GROUP_FORBIDDEN);
        }

        TaskListGroupDO defaultGroup = ensureDefaultGroup(row.getListId(), tenantId, userId);
        List<TaskListItemDO> items = taskListItemMapper.selectList(
                Wrappers.<TaskListItemDO>lambdaQuery()
                        .eq(TaskListItemDO::getListId, row.getListId())
                        .eq(TaskListItemDO::getGroupId, id));
        OffsetDateTime now = OffsetDateTime.now();
        for (TaskListItemDO item : items) {
            item.setGroupId(defaultGroup.getId());
            item.setUpdater(userId);
            item.setUpdateTime(now);
            taskListItemMapper.updateById(item);
        }
        taskListGroupMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(TaskListGroupMoveReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        taskListAccessService.requireCanMutateTasks(request.getListId(), userId);
        ensureDefaultGroup(request.getListId(), tenantId, userId);
        taskAccessService.requireAccessible(request.getTaskId(), userId);

        TaskListGroupDO target = requireGroup(request.getGroupId());
        if (!Objects.equals(target.getListId(), request.getListId())) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_GROUP_NOT_FOUND);
        }

        TaskListItemDO membership = taskListItemMapper.selectOne(
                Wrappers.<TaskListItemDO>lambdaQuery()
                        .eq(TaskListItemDO::getListId, request.getListId())
                        .eq(TaskListItemDO::getTaskId, request.getTaskId())
                        .last("LIMIT 1"));
        if (membership == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_FORBIDDEN);
        }

        int newRank = resolveInsertRank(request.getListId(), target.getId(), request.getBeforeId(), userId);
        membership.setGroupId(target.getId());
        membership.setRank(newRank);
        membership.setUpdater(userId);
        membership.setUpdateTime(OffsetDateTime.now());
        taskListItemMapper.updateById(membership);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long ensureDefaultGroupId(Long listId, Long tenantId, Long operatorUserId) {
        return ensureDefaultGroup(listId, tenantId, operatorUserId).getId();
    }

    private TaskListGroupDO ensureDefaultGroup(Long listId, Long tenantId, Long operatorUserId) {
        TaskListGroupDO existing = taskListGroupMapper.selectOne(
                Wrappers.<TaskListGroupDO>lambdaQuery()
                        .eq(TaskListGroupDO::getListId, listId)
                        .eq(TaskListGroupDO::getIsDefault, 1)
                        .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        OffsetDateTime now = OffsetDateTime.now();
        TaskListGroupDO row = new TaskListGroupDO();
        row.setTenantId(tenantId);
        row.setListId(listId);
        row.setName(DEFAULT_NAME);
        row.setRank(0);
        row.setIsDefault(1);
        row.setCreator(operatorUserId);
        row.setCreateTime(now);
        row.setUpdater(operatorUserId);
        row.setUpdateTime(now);
        taskListGroupMapper.insert(row);
        return row;
    }

    private TaskListGroupDO requireGroup(Long groupId) {
        TaskListGroupDO row = taskListGroupMapper.selectById(groupId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_GROUP_NOT_FOUND);
        }
        return row;
    }

    private int resolveInsertRank(Long listId, Long groupId, Long beforeId, Long userId) {
        if (beforeId == null) {
            return taskListItemMapper.selectList(
                            Wrappers.<TaskListItemDO>lambdaQuery()
                                    .eq(TaskListItemDO::getListId, listId)
                                    .eq(TaskListItemDO::getGroupId, groupId))
                    .stream()
                    .map(TaskListItemDO::getRank)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(-1) + 1;
        }
        TaskListItemDO before = taskListItemMapper.selectOne(
                Wrappers.<TaskListItemDO>lambdaQuery()
                        .eq(TaskListItemDO::getListId, listId)
                        .eq(TaskListItemDO::getTaskId, beforeId)
                        .last("LIMIT 1"));
        if (before == null || !Objects.equals(before.getGroupId(), groupId)) {
            return 0;
        }
        int beforeRank = before.getRank() == null ? 0 : before.getRank();
        List<TaskListItemDO> siblings = taskListItemMapper.selectList(
                Wrappers.<TaskListItemDO>lambdaQuery()
                        .eq(TaskListItemDO::getListId, listId)
                        .eq(TaskListItemDO::getGroupId, groupId)
                        .ge(TaskListItemDO::getRank, beforeRank)
                        .orderByDesc(TaskListItemDO::getRank));
        OffsetDateTime now = OffsetDateTime.now();
        for (TaskListItemDO sibling : siblings) {
            int r = sibling.getRank() == null ? 0 : sibling.getRank();
            sibling.setRank(r + 1);
            sibling.setUpdater(userId);
            sibling.setUpdateTime(now);
            taskListItemMapper.updateById(sibling);
        }
        return beforeRank;
    }

    private TaskListGroupRespVO toGroupVo(TaskListGroupDO row) {
        TaskListGroupRespVO vo = new TaskListGroupRespVO();
        vo.setId(row.getId());
        vo.setListId(row.getListId());
        vo.setName(row.getName());
        vo.setRank(row.getRank());
        vo.setIsDefault(Objects.equals(row.getIsDefault(), 1));
        return vo;
    }

    private TaskListGroupMembershipVO toMembershipVo(TaskListItemDO row, Long defaultGroupId) {
        TaskListGroupMembershipVO vo = new TaskListGroupMembershipVO();
        vo.setTaskId(row.getTaskId());
        vo.setGroupId(row.getGroupId() != null ? row.getGroupId() : defaultGroupId);
        vo.setRank(row.getRank());
        return vo;
    }
}
