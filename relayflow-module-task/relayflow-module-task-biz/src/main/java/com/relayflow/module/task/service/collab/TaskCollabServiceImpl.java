package com.relayflow.module.task.service.collab;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import com.relayflow.module.task.controller.app.vo.TaskActivityRespVO;
import com.relayflow.module.task.controller.app.vo.TaskAssignReqVO;
import com.relayflow.module.task.controller.app.vo.TaskCommentCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskCommentRespVO;
import com.relayflow.module.task.controller.app.vo.TaskFollowerRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.convert.TaskConvert;
import com.relayflow.module.task.dal.dataobject.TaskActivityDO;
import com.relayflow.module.task.dal.dataobject.TaskCommentDO;
import com.relayflow.module.task.dal.dataobject.TaskFollowerDO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskActivityMapper;
import com.relayflow.module.task.dal.mapper.TaskCommentMapper;
import com.relayflow.module.task.dal.mapper.TaskFollowerMapper;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskActivityType;
import com.relayflow.module.task.enums.TaskItemStatus;
import com.relayflow.module.task.service.access.TaskAccessService;
import com.relayflow.module.task.service.assignee.TaskAssigneeService;
import com.relayflow.module.task.service.notify.TaskAssignNotifyService;
import com.relayflow.module.task.service.notify.TaskDueNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCollabServiceImpl implements TaskCollabService {

    private final TaskItemMapper taskItemMapper;
    private final TaskFollowerMapper taskFollowerMapper;
    private final TaskCommentMapper taskCommentMapper;
    private final TaskActivityMapper taskActivityMapper;
    private final TaskAccessService taskAccessService;
    private final TaskActivityRecorder taskActivityRecorder;
    private final TaskAssigneeService taskAssigneeService;
    private final TaskAssignNotifyService taskAssignNotifyService;
    private final TaskDueNotifyService taskDueNotifyService;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;

    @Override
    public void follow(Long taskId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        TaskItemDO task = taskAccessService.requireTask(taskId);
        requireActiveMember(tenantId, userId);

        if (taskAccessService.isFollower(taskId, userId)) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        TaskFollowerDO row = new TaskFollowerDO();
        row.setTenantId(tenantId);
        row.setTaskId(taskId);
        row.setUserId(userId);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        taskFollowerMapper.insert(row);
        taskActivityRecorder.record(task, userId, TaskActivityType.FOLLOWER_ADDED, "关注了任务");
    }

    @Override
    public void unfollow(Long taskId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskItemDO task = taskAccessService.requireTask(taskId);
        TaskFollowerDO existing = taskFollowerMapper.selectOne(
                Wrappers.<TaskFollowerDO>lambdaQuery()
                        .eq(TaskFollowerDO::getTaskId, taskId)
                        .eq(TaskFollowerDO::getUserId, userId)
                        .last("LIMIT 1"));
        if (existing == null) {
            return;
        }
        taskFollowerMapper.deleteById(existing.getId());
        taskActivityRecorder.record(task, userId, TaskActivityType.FOLLOWER_REMOVED, "取消关注");
    }

    @Override
    public List<TaskFollowerRespVO> listFollowers(Long taskId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        taskAccessService.requireAccessible(taskId, userId);
        List<TaskFollowerDO> rows = taskFollowerMapper.selectList(
                Wrappers.<TaskFollowerDO>lambdaQuery()
                        .eq(TaskFollowerDO::getTaskId, taskId)
                        .orderByAsc(TaskFollowerDO::getCreateTime));
        List<TaskFollowerRespVO> list = new ArrayList<>(rows.size());
        for (TaskFollowerDO row : rows) {
            TaskFollowerRespVO vo = new TaskFollowerRespVO();
            vo.setUserId(row.getUserId());
            String nickname = resolveNickname(row.getUserId());
            vo.setNickname(nickname);
            vo.setAvatarText(avatarText(nickname));
            vo.setFollowTime(row.getCreateTime());
            list.add(vo);
        }
        return list;
    }

    @Override
    public PageResult<TaskItemRespVO> pageFollowing(TaskItemPageReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        List<TaskFollowerDO> follows = taskFollowerMapper.selectList(
                Wrappers.<TaskFollowerDO>lambdaQuery()
                        .eq(TaskFollowerDO::getUserId, userId)
                        .orderByDesc(TaskFollowerDO::getCreateTime));
        if (follows.isEmpty()) {
            return PageResult.of(List.of(), 0L);
        }
        List<Long> taskIds = follows.stream().map(TaskFollowerDO::getTaskId).distinct().toList();
        String status = normalizeStatusFilter(request.getStatus());
        Page<TaskItemDO> page = taskItemMapper.selectPage(
                new Page<>(request.getPageNo(), request.getPageSize()),
                Wrappers.<TaskItemDO>lambdaQuery()
                        .in(TaskItemDO::getId, taskIds)
                        .isNull(TaskItemDO::getParentId)
                        .eq(StringUtils.hasText(status), TaskItemDO::getStatus, status)
                        .orderByDesc(TaskItemDO::getCreateTime));
        List<TaskItemRespVO> list = TaskConvert.INSTANCE.toRespList(page.getRecords());
        fillSubtaskCounts(list);
        fillAssigneeIds(list);
        return PageResult.of(list, page.getTotal());
    }

    @Override
    public void assign(TaskAssignReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        TaskItemDO task = taskAccessService.requireEditable(request.getId(), userId);
        Long assigneeId = request.getAssigneeId();
        // Compat: single-assignee assign = replace set with one element
        taskAssigneeService.replaceAssignees(task, userId, tenantId, List.of(assigneeId), true, true);
        taskDueNotifyService.pushIfDueSoon(task);
    }

    @Override
    public List<TaskCommentRespVO> listComments(Long taskId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        taskAccessService.requireAccessible(taskId, userId);
        List<TaskCommentDO> rows = taskCommentMapper.selectList(
                Wrappers.<TaskCommentDO>lambdaQuery()
                        .eq(TaskCommentDO::getTaskId, taskId)
                        .orderByAsc(TaskCommentDO::getCreateTime));
        return rows.stream().map(this::toCommentVo).toList();
    }

    @Override
    public Long createComment(TaskCommentCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new ServiceException(ErrorCodeConstants.TASK_COMMENT_EMPTY);
        }
        TaskItemDO task = taskAccessService.requireAccessible(request.getTaskId(), userId);
        OffsetDateTime now = OffsetDateTime.now();
        TaskCommentDO row = new TaskCommentDO();
        row.setTenantId(tenantId);
        row.setTaskId(task.getId());
        row.setAuthorId(userId);
        row.setContent(content);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        taskCommentMapper.insert(row);
        taskActivityRecorder.record(task, userId, TaskActivityType.COMMENTED, "添加了评论");
        return row.getId();
    }

    @Override
    public List<TaskActivityRespVO> listActivities(Long taskId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        taskAccessService.requireAccessible(taskId, userId);
        List<TaskActivityDO> rows = taskActivityMapper.selectList(
                Wrappers.<TaskActivityDO>lambdaQuery()
                        .eq(TaskActivityDO::getTaskId, taskId)
                        .orderByDesc(TaskActivityDO::getCreateTime));
        return rows.stream().map(this::toActivityVo).toList();
    }

    @Override
    public List<TaskActivityRespVO> listActivityFeed(int limit) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        int safeLimit = clampFeedLimit(limit);
        Set<Long> relatedTaskIds = relatedTaskIds(userId);
        if (relatedTaskIds.isEmpty()) {
            return List.of();
        }
        List<TaskActivityDO> rows = taskActivityMapper.selectList(
                Wrappers.<TaskActivityDO>lambdaQuery()
                        .in(TaskActivityDO::getTaskId, relatedTaskIds)
                        .orderByDesc(TaskActivityDO::getCreateTime)
                        .last("LIMIT " + safeLimit));
        return rows.stream().map(this::toActivityVo).toList();
    }

    private Set<Long> relatedTaskIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        ids.addAll(taskAssigneeService.listTaskIdsByUserId(userId));
        List<TaskItemDO> created = taskItemMapper.selectList(
                Wrappers.<TaskItemDO>lambdaQuery()
                        .eq(TaskItemDO::getCreatorId, userId)
                        .select(TaskItemDO::getId));
        for (TaskItemDO row : created) {
            ids.add(row.getId());
        }
        List<TaskFollowerDO> follows = taskFollowerMapper.selectList(
                Wrappers.<TaskFollowerDO>lambdaQuery()
                        .eq(TaskFollowerDO::getUserId, userId)
                        .select(TaskFollowerDO::getTaskId));
        for (TaskFollowerDO row : follows) {
            ids.add(row.getTaskId());
        }
        return ids;
    }

    private void fillAssigneeIds(List<TaskItemRespVO> roots) {
        if (roots == null || roots.isEmpty()) {
            return;
        }
        Set<Long> ids = roots.stream().map(TaskItemRespVO::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, List<Long>> map = taskAssigneeService.mapAssigneeIdsByTaskIds(ids);
        for (TaskItemRespVO root : roots) {
            List<Long> assigneeIds = map.getOrDefault(root.getId(), List.of());
            root.setAssigneeIds(assigneeIds);
            root.setAssigneeId(assigneeIds.isEmpty() ? null : assigneeIds.get(0));
        }
    }

    private void requireActiveMember(Long tenantId, Long userId) {
        Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, List.of(userId));
        if (active == null || !active.contains(userId)) {
            throw new ServiceException(ErrorCodeConstants.TASK_ASSIGNEE_NOT_MEMBER);
        }
    }

    private void fillSubtaskCounts(List<TaskItemRespVO> roots) {
        if (roots == null || roots.isEmpty()) {
            return;
        }
        Set<Long> ids = roots.stream().map(TaskItemRespVO::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        List<TaskItemDO> children = taskItemMapper.selectList(
                Wrappers.<TaskItemDO>lambdaQuery().in(TaskItemDO::getParentId, ids));
        Map<Long, int[]> counts = new HashMap<>();
        if (children != null) {
            for (TaskItemDO child : children) {
                int[] pair = counts.computeIfAbsent(child.getParentId(), k -> new int[]{0, 0});
                pair[0]++;
                if (TaskItemStatus.DONE.equals(child.getStatus())) {
                    pair[1]++;
                }
            }
        }
        for (TaskItemRespVO root : roots) {
            int[] pair = counts.get(root.getId());
            root.setSubtaskTotal(pair == null ? 0 : pair[0]);
            root.setSubtaskDoneCount(pair == null ? 0 : pair[1]);
        }
    }

    private TaskCommentRespVO toCommentVo(TaskCommentDO row) {
        TaskCommentRespVO vo = new TaskCommentRespVO();
        vo.setId(row.getId());
        vo.setTaskId(row.getTaskId());
        vo.setAuthorId(row.getAuthorId());
        vo.setAuthorNickname(resolveNickname(row.getAuthorId()));
        vo.setContent(row.getContent());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    private TaskActivityRespVO toActivityVo(TaskActivityDO row) {
        TaskActivityRespVO vo = new TaskActivityRespVO();
        vo.setId(row.getId());
        vo.setTaskId(row.getTaskId());
        vo.setTaskTitle(row.getTaskTitle());
        vo.setActorId(row.getActorId());
        vo.setActorNickname(resolveNickname(row.getActorId()));
        vo.setType(row.getType());
        vo.setSummary(row.getSummary());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    private String resolveNickname(Long userId) {
        if (userId == null) {
            return "用户";
        }
        try {
            UserBasicDTO basic = userApi.getUserBasic(userId);
            if (basic != null && StringUtils.hasText(basic.getNickname())) {
                return basic.getNickname();
            }
            if (basic != null && StringUtils.hasText(basic.getUsername())) {
                return basic.getUsername();
            }
        } catch (Exception ex) {
            log.warn("Resolve nickname failed (best-effort): userId={}", userId, ex);
        }
        return "用户 " + userId;
    }

    private static String avatarText(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return "?";
        }
        return nickname.trim().substring(0, 1);
    }

    private static int clampFeedLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }

    private String normalizeStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (TaskItemStatus.isValid(normalized)) {
            return normalized;
        }
        return null;
    }
}
