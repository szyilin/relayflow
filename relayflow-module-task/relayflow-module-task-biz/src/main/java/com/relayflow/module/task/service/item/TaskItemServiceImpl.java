package com.relayflow.module.task.service.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.task.controller.app.vo.TaskItemBoardMoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskSubtaskCreateReqVO;
import com.relayflow.module.task.convert.TaskConvert;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskItemExtMapper;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskActivityType;
import com.relayflow.module.task.enums.TaskItemStatus;
import com.relayflow.module.task.service.access.TaskAccessService;
import com.relayflow.module.task.service.access.TaskListAccessService;
import com.relayflow.module.task.service.collab.TaskActivityRecorder;
import com.relayflow.module.task.service.notify.TaskDueNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskItemServiceImpl implements TaskItemService {

    private final TaskItemMapper taskItemMapper;
    private final TaskItemExtMapper taskItemExtMapper;
    private final TaskDueNotifyService taskDueNotifyService;
    private final TaskAccessService taskAccessService;
    private final TaskListAccessService taskListAccessService;
    private final TaskActivityRecorder taskActivityRecorder;

    @Override
    public PageResult<TaskItemRespVO> pageMyTasks(TaskItemPageReqVO request) {
        return pageMyTasks(SecurityFrameworkUtils.requireLoginUserId(), request);
    }

    private PageResult<TaskItemRespVO> pageMyTasks(Long userId, TaskItemPageReqVO request) {
        if (request.getListId() != null) {
            return pageListTasks(userId, request);
        }
        String status = normalizeStatusFilter(request.getStatus());
        String scope = normalizePageScope(request.getScope());
        if ("ALL".equals(scope)) {
            Page<TaskItemDO> page = new Page<>(request.getPageNo(), request.getPageSize());
            IPage<TaskItemDO> result = taskItemExtMapper.selectVisibleUnionPage(
                    page, userId, StringUtils.hasText(status) ? status : null);
            taskDueNotifyService.compensateMissingDueReminders(result.getRecords());
            List<TaskItemRespVO> list = TaskConvert.INSTANCE.toRespList(result.getRecords());
            fillSubtaskCounts(list);
            return PageResult.of(list, result.getTotal());
        }
        if ("ASSIGNED_BY_ME".equals(scope)) {
            Page<TaskItemDO> page = taskItemMapper.selectPage(
                    new Page<>(request.getPageNo(), request.getPageSize()),
                    Wrappers.<TaskItemDO>lambdaQuery()
                            .eq(TaskItemDO::getAssignerId, userId)
                            .and(w -> w.isNull(TaskItemDO::getAssigneeId)
                                    .or()
                                    .ne(TaskItemDO::getAssigneeId, userId))
                            .isNull(TaskItemDO::getParentId)
                            .eq(StringUtils.hasText(status), TaskItemDO::getStatus, status)
                            .orderByDesc(TaskItemDO::getCreateTime));
            taskDueNotifyService.compensateMissingDueReminders(page.getRecords());
            List<TaskItemRespVO> list = TaskConvert.INSTANCE.toRespList(page.getRecords());
            fillSubtaskCounts(list);
            return PageResult.of(list, page.getTotal());
        }
        boolean byCreator = "CREATOR".equals(scope);
        Page<TaskItemDO> page = taskItemMapper.selectPage(
                new Page<>(request.getPageNo(), request.getPageSize()),
                Wrappers.<TaskItemDO>lambdaQuery()
                        .eq(byCreator, TaskItemDO::getCreatorId, userId)
                        .eq(!byCreator, TaskItemDO::getAssigneeId, userId)
                        .isNull(TaskItemDO::getParentId)
                        .eq(StringUtils.hasText(status), TaskItemDO::getStatus, status)
                        .orderByDesc(TaskItemDO::getCreateTime));
        taskDueNotifyService.compensateMissingDueReminders(page.getRecords());
        List<TaskItemRespVO> list = TaskConvert.INSTANCE.toRespList(page.getRecords());
        fillSubtaskCounts(list);
        return PageResult.of(list, page.getTotal());
    }

    private static String normalizePageScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return "ASSIGNEE";
        }
        String normalized = scope.trim().toUpperCase();
        return switch (normalized) {
            case "CREATOR", "ALL", "ASSIGNED_BY_ME", "ASSIGNEE" -> normalized;
            default -> "ASSIGNEE";
        };
    }

    private PageResult<TaskItemRespVO> pageListTasks(Long userId, TaskItemPageReqVO request) {
        taskListAccessService.requireReadable(request.getListId(), userId);
        String status = normalizeStatusFilter(request.getStatus());
        Page<TaskItemDO> page = taskItemMapper.selectPage(
                new Page<>(request.getPageNo(), request.getPageSize()),
                Wrappers.<TaskItemDO>lambdaQuery()
                        .eq(TaskItemDO::getListId, request.getListId())
                        .isNull(TaskItemDO::getParentId)
                        .eq(StringUtils.hasText(status), TaskItemDO::getStatus, status)
                        .orderByAsc(TaskItemDO::getBoardRank)
                        .orderByDesc(TaskItemDO::getCreateTime));
        List<TaskItemRespVO> list = TaskConvert.INSTANCE.toRespList(page.getRecords());
        fillSubtaskCounts(list);
        return PageResult.of(list, page.getTotal());
    }

    @Override
    public List<TaskItemRespVO> searchMyTasks(String keyword, int limit) {
        return TaskConvert.INSTANCE.toRespList(
                searchMyTasks(SecurityFrameworkUtils.requireLoginUserId(), keyword, limit));
    }

    @Override
    public List<TaskItemDO> searchMyTasks(Long userId, String keyword, int limit) {
        int safeLimit = clampSearchLimit(limit);
        String trimmed = keyword.trim();
        return taskItemMapper.selectList(
                Wrappers.<TaskItemDO>lambdaQuery()
                        .eq(TaskItemDO::getAssigneeId, userId)
                        .isNull(TaskItemDO::getParentId)
                        .like(TaskItemDO::getTitle, trimmed)
                        .orderByDesc(TaskItemDO::getCreateTime)
                        .last("LIMIT " + safeLimit));
    }

    @Override
    public List<TaskItemRespVO> listDueRange(OffsetDateTime from, OffsetDateTime to, int limit) {
        return TaskConvert.INSTANCE.toRespList(
                listDueRange(SecurityFrameworkUtils.requireLoginUserId(), from, to, limit));
    }

    @Override
    public List<TaskItemDO> listDueRange(Long userId, OffsetDateTime from, OffsetDateTime to, int limit) {
        if (from == null || to == null || !from.isBefore(to)) {
            return List.of();
        }
        int safeLimit = clampDueRangeLimit(limit);
        return taskItemMapper.selectList(
                Wrappers.<TaskItemDO>lambdaQuery()
                        .eq(TaskItemDO::getAssigneeId, userId)
                        .in(TaskItemDO::getStatus, TaskItemStatus.TODO, TaskItemStatus.IN_PROGRESS)
                        .isNotNull(TaskItemDO::getDueTime)
                        .ge(TaskItemDO::getDueTime, from)
                        .lt(TaskItemDO::getDueTime, to)
                        .orderByAsc(TaskItemDO::getDueTime)
                        .last("LIMIT " + safeLimit));
    }

    @Override
    public TaskItemRespVO getTask(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskItemDO row = taskAccessService.requireAccessible(id, userId);
        TaskItemRespVO vo = TaskConvert.INSTANCE.toResp(row);
        if (row.getParentId() == null) {
            fillSubtaskCounts(List.of(vo));
        }
        return vo;
    }

    private static int clampSearchLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 10);
    }

    private static int clampDueRangeLimit(int limit) {
        if (limit <= 0) {
            return 200;
        }
        return Math.min(limit, 200);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(TaskItemCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        validateTimeRange(request.getStartTime(), request.getDueTime());
        return createRootTask(userId, tenantId, request);
    }

    private Long createRootTask(Long userId, Long tenantId, TaskItemCreateReqVO request) {
        String title = request.getTitle().trim();
        OffsetDateTime now = OffsetDateTime.now();
        Long listId = request.getListId();
        if (listId != null) {
            taskListAccessService.requireCanMutateTasks(listId, userId);
        }

        TaskItemDO row = new TaskItemDO();
        row.setTenantId(tenantId);
        row.setTitle(title);
        row.setAssigneeId(userId);
        row.setCreatorId(userId);
        row.setStartTime(request.getStartTime());
        row.setDueTime(request.getDueTime());
        row.setRemindBeforeMinutes(request.getRemindBeforeMinutes());
        row.setParentId(null);
        row.setListId(listId);
        row.setStatus(TaskItemStatus.TODO);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        taskItemMapper.insert(row);
        taskDueNotifyService.pushIfDueSoon(row);
        taskActivityRecorder.record(row, userId, TaskActivityType.CREATED, "创建了任务");
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(TaskItemUpdateReqVO request) {
        updateTask(SecurityFrameworkUtils.requireLoginUserId(), request);
    }

    private void updateTask(Long userId, TaskItemUpdateReqVO request) {
        TaskItemDO row = taskAccessService.requireEditable(request.getId(), userId);

        OffsetDateTime nextStart = request.isStartTimePresent() ? request.getStartTime() : row.getStartTime();
        OffsetDateTime nextDue = request.isDueTimePresent() ? request.getDueTime() : row.getDueTime();
        validateTimeRange(nextStart, nextDue);

        if (request.isTitlePresent() && StringUtils.hasText(request.getTitle())) {
            row.setTitle(request.getTitle().trim());
        }
        if (request.isStartTimePresent()) {
            row.setStartTime(request.getStartTime());
        }
        if (request.isDueTimePresent()) {
            row.setDueTime(request.getDueTime());
        }
        if (request.isRemindBeforeMinutesPresent()) {
            row.setRemindBeforeMinutes(request.getRemindBeforeMinutes());
        }
        if (request.isDescriptionPresent()) {
            row.setDescription(request.getDescription());
        }
        row.setUpdater(userId);
        row.setUpdateTime(OffsetDateTime.now());
        taskItemMapper.updateById(row);
        taskDueNotifyService.pushIfDueSoon(row);
        taskActivityRecorder.record(row, userId, TaskActivityType.FIELD_CHANGED, "更新了任务详情");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleDone(TaskItemToggleDoneReqVO request) {
        toggleDone(SecurityFrameworkUtils.requireLoginUserId(), request);
    }

    private void toggleDone(Long userId, TaskItemToggleDoneReqVO request) {
        TaskItemDO row = taskAccessService.requireEditable(request.getId(), userId);
        String nextStatus = Boolean.TRUE.equals(request.getDone()) ? TaskItemStatus.DONE : TaskItemStatus.TODO;
        if (Objects.equals(nextStatus, row.getStatus())) {
            return;
        }
        row.setStatus(nextStatus);
        row.setUpdater(userId);
        row.setUpdateTime(OffsetDateTime.now());
        taskItemMapper.updateById(row);
        if (row.getParentId() != null && TaskItemStatus.DONE.equals(nextStatus)) {
            TaskItemDO parent = taskItemMapper.selectById(row.getParentId());
            if (parent != null) {
                taskActivityRecorder.record(parent, userId, TaskActivityType.SUBTASK_DONE, "完成了子任务");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        deleteTask(SecurityFrameworkUtils.requireLoginUserId(), id);
    }

    private void deleteTask(Long userId, Long id) {
        TaskItemDO row = taskAccessService.requireEditable(id, userId);
        if (row.getParentId() == null) {
            List<TaskItemDO> children = taskItemMapper.selectList(
                    Wrappers.<TaskItemDO>lambdaQuery().eq(TaskItemDO::getParentId, id));
            for (TaskItemDO child : children) {
                if (Objects.equals(child.getAssigneeId(), userId)
                        || Objects.equals(child.getCreatorId(), userId)) {
                    taskItemMapper.deleteById(child.getId());
                }
            }
        }
        taskItemMapper.deleteById(id);
    }

    @Override
    public List<TaskItemRespVO> listSubtasks(Long parentId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        taskAccessService.requireAccessible(parentId, userId);
        List<TaskItemDO> rows = taskItemMapper.selectList(
                Wrappers.<TaskItemDO>lambdaQuery()
                        .eq(TaskItemDO::getParentId, parentId)
                        .orderByAsc(TaskItemDO::getCreateTime));
        return TaskConvert.INSTANCE.toRespList(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSubtask(TaskSubtaskCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        TaskItemDO parent = taskAccessService.requireEditable(request.getParentId(), userId);
        if (parent.getParentId() != null) {
            throw new ServiceException(ErrorCodeConstants.TASK_SUBTASK_DEPTH_EXCEEDED);
        }
        OffsetDateTime now = OffsetDateTime.now();
        TaskItemDO row = new TaskItemDO();
        row.setTenantId(tenantId);
        row.setTitle(request.getTitle().trim());
        row.setAssigneeId(userId);
        row.setCreatorId(userId);
        row.setParentId(parent.getId());
        row.setListId(parent.getListId());
        row.setStatus(TaskItemStatus.TODO);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        taskItemMapper.insert(row);
        taskActivityRecorder.record(parent, userId, TaskActivityType.SUBTASK_CREATED,
                "添加了子任务「" + row.getTitle() + "」");
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void boardMove(TaskItemBoardMoveReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskItemDO row = taskAccessService.requireEditable(request.getId(), userId);
        if (row.getParentId() != null) {
            throw new ServiceException(ErrorCodeConstants.TASK_FORBIDDEN);
        }
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase();
        if (!TaskItemStatus.isValid(status)) {
            throw new ServiceException(ErrorCodeConstants.TASK_FORBIDDEN);
        }
        Integer boardRank = request.getBoardRank();
        if (boardRank == null) {
            boardRank = nextBoardRank(row.getListId(), status, row.getId());
        }
        String previous = row.getStatus();
        row.setStatus(status);
        row.setBoardRank(boardRank);
        row.setUpdater(userId);
        row.setUpdateTime(OffsetDateTime.now());
        taskItemMapper.updateById(row);
        if (!Objects.equals(previous, status)) {
            taskActivityRecorder.record(row, userId, TaskActivityType.FIELD_CHANGED,
                    "将状态改为「" + status + "」");
        }
        taskDueNotifyService.pushIfDueSoon(row);
    }

    private Integer nextBoardRank(Long listId, String status, Long excludeId) {
        var query = Wrappers.<TaskItemDO>lambdaQuery()
                .eq(TaskItemDO::getStatus, status)
                .isNull(TaskItemDO::getParentId)
                .ne(excludeId != null, TaskItemDO::getId, excludeId)
                .orderByDesc(TaskItemDO::getBoardRank)
                .last("LIMIT 1");
        if (listId != null) {
            query.eq(TaskItemDO::getListId, listId);
        } else {
            query.isNull(TaskItemDO::getListId);
        }
        TaskItemDO last = taskItemMapper.selectOne(query);
        int base = last != null && last.getBoardRank() != null ? last.getBoardRank() : 0;
        return base + 1000;
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
        if (children == null || children.isEmpty()) {
            for (TaskItemRespVO root : roots) {
                root.setSubtaskTotal(0);
                root.setSubtaskDoneCount(0);
            }
            return;
        }
        Map<Long, int[]> counts = new HashMap<>();
        for (TaskItemDO child : children) {
            int[] pair = counts.computeIfAbsent(child.getParentId(), k -> new int[]{0, 0});
            pair[0]++;
            if (TaskItemStatus.DONE.equals(child.getStatus())) {
                pair[1]++;
            }
        }
        for (TaskItemRespVO root : roots) {
            int[] pair = counts.get(root.getId());
            if (pair == null) {
                root.setSubtaskTotal(0);
                root.setSubtaskDoneCount(0);
            } else {
                root.setSubtaskTotal(pair[0]);
                root.setSubtaskDoneCount(pair[1]);
            }
        }
    }

    private static void validateTimeRange(OffsetDateTime start, OffsetDateTime due) {
        if (start != null && due != null && start.isAfter(due)) {
            throw new ServiceException(ErrorCodeConstants.TASK_INVALID_TIME_RANGE);
        }
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
