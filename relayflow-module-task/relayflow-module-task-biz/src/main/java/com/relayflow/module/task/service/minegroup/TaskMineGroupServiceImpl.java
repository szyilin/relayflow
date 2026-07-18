package com.relayflow.module.task.service.minegroup;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupMembershipVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupMoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupRespVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupUpdateReqVO;
import com.relayflow.module.task.dal.dataobject.TaskMineGroupDO;
import com.relayflow.module.task.dal.dataobject.TaskMineGroupItemDO;
import com.relayflow.module.task.dal.mapper.TaskMineGroupItemMapper;
import com.relayflow.module.task.dal.mapper.TaskMineGroupMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.service.access.TaskAccessService;
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
public class TaskMineGroupServiceImpl implements TaskMineGroupService {

    private static final String DEFAULT_NAME = "默认";

    private final TaskMineGroupMapper taskMineGroupMapper;
    private final TaskMineGroupItemMapper taskMineGroupItemMapper;
    private final TaskAccessService taskAccessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskMineGroupListRespVO list() {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        ensureDefaultGroup(userId, tenantId);

        List<TaskMineGroupDO> groups = taskMineGroupMapper.selectList(
                Wrappers.<TaskMineGroupDO>lambdaQuery()
                        .eq(TaskMineGroupDO::getUserId, userId)
                        .orderByAsc(TaskMineGroupDO::getRank)
                        .orderByAsc(TaskMineGroupDO::getId));

        List<TaskMineGroupItemDO> items = taskMineGroupItemMapper.selectList(
                Wrappers.<TaskMineGroupItemDO>lambdaQuery()
                        .eq(TaskMineGroupItemDO::getUserId, userId)
                        .orderByAsc(TaskMineGroupItemDO::getRank)
                        .orderByAsc(TaskMineGroupItemDO::getId));

        TaskMineGroupListRespVO resp = new TaskMineGroupListRespVO();
        resp.setGroups(groups.stream().map(this::toGroupVo).toList());
        resp.setMemberships(items.stream().map(this::toMembershipVo).toList());
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskMineGroupRespVO create(TaskMineGroupCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        ensureDefaultGroup(userId, tenantId);

        String name = request.getName() == null ? "" : request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new ServiceException(ErrorCodeConstants.TASK_MINE_GROUP_NAME_EMPTY);
        }

        int nextRank = taskMineGroupMapper.selectList(
                        Wrappers.<TaskMineGroupDO>lambdaQuery()
                                .eq(TaskMineGroupDO::getUserId, userId))
                .stream()
                .map(TaskMineGroupDO::getRank)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        OffsetDateTime now = OffsetDateTime.now();
        TaskMineGroupDO row = new TaskMineGroupDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setName(name);
        row.setRank(nextRank);
        row.setIsDefault(0);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        taskMineGroupMapper.insert(row);
        return toGroupVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TaskMineGroupUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskMineGroupDO row = requireOwnedGroup(request.getId(), userId);

        boolean changed = false;
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (!StringUtils.hasText(name)) {
                throw new ServiceException(ErrorCodeConstants.TASK_MINE_GROUP_NAME_EMPTY);
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
        taskMineGroupMapper.updateById(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        TaskMineGroupDO row = requireOwnedGroup(id, userId);
        if (Objects.equals(row.getIsDefault(), 1)) {
            throw new ServiceException(ErrorCodeConstants.TASK_MINE_GROUP_FORBIDDEN);
        }

        TaskMineGroupDO defaultGroup = ensureDefaultGroup(userId, tenantId);
        List<TaskMineGroupItemDO> items = taskMineGroupItemMapper.selectList(
                Wrappers.<TaskMineGroupItemDO>lambdaQuery()
                        .eq(TaskMineGroupItemDO::getUserId, userId)
                        .eq(TaskMineGroupItemDO::getGroupId, id));
        OffsetDateTime now = OffsetDateTime.now();
        for (TaskMineGroupItemDO item : items) {
            item.setGroupId(defaultGroup.getId());
            item.setUpdater(userId);
            item.setUpdateTime(now);
            taskMineGroupItemMapper.updateById(item);
        }
        taskMineGroupMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(TaskMineGroupMoveReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        ensureDefaultGroup(userId, tenantId);
        taskAccessService.requireAccessible(request.getTaskId(), userId);
        TaskMineGroupDO target = requireOwnedGroup(request.getGroupId(), userId);

        int newRank = resolveInsertRank(userId, target.getId(), request.getBeforeId());
        upsertMembership(userId, tenantId, request.getTaskId(), target.getId(), newRank);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureMembershipInDefault(Long taskId, Long userId, Long tenantId) {
        if (taskId == null || userId == null || tenantId == null) {
            return;
        }
        TaskMineGroupItemDO existing = findMembership(userId, taskId);
        if (existing != null) {
            return;
        }
        TaskMineGroupDO defaultGroup = ensureDefaultGroup(userId, tenantId);
        upsertMembership(userId, tenantId, taskId, defaultGroup.getId(), 0);
    }

    private TaskMineGroupDO ensureDefaultGroup(Long userId, Long tenantId) {
        TaskMineGroupDO existing = taskMineGroupMapper.selectOne(
                Wrappers.<TaskMineGroupDO>lambdaQuery()
                        .eq(TaskMineGroupDO::getUserId, userId)
                        .eq(TaskMineGroupDO::getIsDefault, 1)
                        .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        OffsetDateTime now = OffsetDateTime.now();
        TaskMineGroupDO row = new TaskMineGroupDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setName(DEFAULT_NAME);
        row.setRank(0);
        row.setIsDefault(1);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        taskMineGroupMapper.insert(row);
        return row;
    }

    private TaskMineGroupDO requireOwnedGroup(Long groupId, Long userId) {
        TaskMineGroupDO row = taskMineGroupMapper.selectById(groupId);
        if (row == null || !Objects.equals(row.getUserId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.TASK_MINE_GROUP_NOT_FOUND);
        }
        return row;
    }

    private TaskMineGroupItemDO findMembership(Long userId, Long taskId) {
        return taskMineGroupItemMapper.selectOne(
                Wrappers.<TaskMineGroupItemDO>lambdaQuery()
                        .eq(TaskMineGroupItemDO::getUserId, userId)
                        .eq(TaskMineGroupItemDO::getTaskId, taskId)
                        .last("LIMIT 1"));
    }

    private void upsertMembership(Long userId, Long tenantId, Long taskId, Long groupId, int rank) {
        OffsetDateTime now = OffsetDateTime.now();
        TaskMineGroupItemDO existing = findMembership(userId, taskId);
        if (existing == null) {
            TaskMineGroupItemDO row = new TaskMineGroupItemDO();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setTaskId(taskId);
            row.setGroupId(groupId);
            row.setRank(rank);
            row.setCreator(userId);
            row.setCreateTime(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            taskMineGroupItemMapper.insert(row);
            return;
        }
        existing.setGroupId(groupId);
        existing.setRank(rank);
        existing.setUpdater(userId);
        existing.setUpdateTime(now);
        taskMineGroupItemMapper.updateById(existing);
    }

    private int resolveInsertRank(Long userId, Long groupId, Long beforeId) {
        if (beforeId == null) {
            return taskMineGroupItemMapper.selectList(
                            Wrappers.<TaskMineGroupItemDO>lambdaQuery()
                                    .eq(TaskMineGroupItemDO::getUserId, userId)
                                    .eq(TaskMineGroupItemDO::getGroupId, groupId))
                    .stream()
                    .map(TaskMineGroupItemDO::getRank)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(-1) + 1;
        }
        TaskMineGroupItemDO before = findMembership(userId, beforeId);
        if (before == null || !Objects.equals(before.getGroupId(), groupId)) {
            return 0;
        }
        int beforeRank = before.getRank() == null ? 0 : before.getRank();
        List<TaskMineGroupItemDO> siblings = taskMineGroupItemMapper.selectList(
                Wrappers.<TaskMineGroupItemDO>lambdaQuery()
                        .eq(TaskMineGroupItemDO::getUserId, userId)
                        .eq(TaskMineGroupItemDO::getGroupId, groupId)
                        .ge(TaskMineGroupItemDO::getRank, beforeRank)
                        .orderByDesc(TaskMineGroupItemDO::getRank));
        OffsetDateTime now = OffsetDateTime.now();
        for (TaskMineGroupItemDO sibling : siblings) {
            int r = sibling.getRank() == null ? 0 : sibling.getRank();
            sibling.setRank(r + 1);
            sibling.setUpdater(userId);
            sibling.setUpdateTime(now);
            taskMineGroupItemMapper.updateById(sibling);
        }
        return beforeRank;
    }

    private TaskMineGroupRespVO toGroupVo(TaskMineGroupDO row) {
        TaskMineGroupRespVO vo = new TaskMineGroupRespVO();
        vo.setId(row.getId());
        vo.setName(row.getName());
        vo.setRank(row.getRank());
        vo.setIsDefault(Objects.equals(row.getIsDefault(), 1));
        return vo;
    }

    private TaskMineGroupMembershipVO toMembershipVo(TaskMineGroupItemDO row) {
        TaskMineGroupMembershipVO vo = new TaskMineGroupMembershipVO();
        vo.setTaskId(row.getTaskId());
        vo.setGroupId(row.getGroupId());
        vo.setRank(row.getRank());
        return vo;
    }
}
