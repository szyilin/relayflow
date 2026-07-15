package com.relayflow.module.task.service.item;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.convert.TaskConvert;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mysql.TaskItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskItemStatus;
import com.relayflow.module.task.service.notify.TaskDueNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskItemServiceImpl implements TaskItemService {

    private final TaskItemMapper taskItemMapper;
    private final TaskDueNotifyService taskDueNotifyService;

    @Override
    public PageResult<TaskItemRespVO> pageMyTasks(TaskItemPageReqVO request) {
        return pageMyTasks(SecurityFrameworkUtils.requireLoginUserId(), request);
    }

    private PageResult<TaskItemRespVO> pageMyTasks(Long userId, TaskItemPageReqVO request) {
        String status = normalizeStatusFilter(request.getStatus());
        Page<TaskItemDO> page = taskItemMapper.selectPage(
                new Page<>(request.getPageNo(), request.getPageSize()),
                Wrappers.<TaskItemDO>lambdaQuery()
                        .eq(TaskItemDO::getAssigneeId, userId)
                        .eq(StringUtils.hasText(status), TaskItemDO::getStatus, status)
                        .orderByDesc(TaskItemDO::getCreateTime));
        taskDueNotifyService.compensateMissingDueReminders(page.getRecords());
        return PageResult.of(TaskConvert.INSTANCE.toRespList(page.getRecords()), page.getTotal());
    }

    @Override
    public List<TaskItemRespVO> searchMyTasks(String keyword, int limit) {
        return searchMyTasks(SecurityFrameworkUtils.requireLoginUserId(), keyword, limit);
    }

    @Override
    public List<TaskItemRespVO> searchMyTasks(Long userId, String keyword, int limit) {
        int safeLimit = clampSearchLimit(limit);
        String trimmed = keyword.trim();
        List<TaskItemDO> rows = taskItemMapper.selectList(
                Wrappers.<TaskItemDO>lambdaQuery()
                        .eq(TaskItemDO::getAssigneeId, userId)
                        .like(TaskItemDO::getTitle, trimmed)
                        .orderByDesc(TaskItemDO::getCreateTime)
                        .last("LIMIT " + safeLimit));
        return TaskConvert.INSTANCE.toRespList(rows);
    }

    private static int clampSearchLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 10);
    }

    @Override
    public Long createTask(TaskItemCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        return createTask(userId, tenantId, request);
    }

    private Long createTask(Long userId, Long tenantId, TaskItemCreateReqVO request) {
        String title = request.getTitle().trim();
        OffsetDateTime now = OffsetDateTime.now();

        TaskItemDO row = new TaskItemDO();
        row.setTenantId(tenantId);
        row.setTitle(title);
        row.setAssigneeId(userId);
        row.setCreatorId(userId);
        row.setDueTime(request.getDueTime());
        row.setStatus(TaskItemStatus.TODO);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        taskItemMapper.insert(row);
        taskDueNotifyService.pushIfDueSoon(row);
        return row.getId();
    }

    @Override
    public void updateTask(TaskItemUpdateReqVO request) {
        updateTask(SecurityFrameworkUtils.requireLoginUserId(), request);
    }

    private void updateTask(Long userId, TaskItemUpdateReqVO request) {
        TaskItemDO row = requireOwnedTask(request.getId(), userId);
        boolean changed = false;

        if (StringUtils.hasText(request.getTitle())) {
            row.setTitle(request.getTitle().trim());
            changed = true;
        }
        if (request.getDueTime() != null) {
            row.setDueTime(request.getDueTime());
            changed = true;
        }
        if (!changed) {
            return;
        }
        row.setUpdater(userId);
        row.setUpdateTime(OffsetDateTime.now());
        taskItemMapper.updateById(row);
        taskDueNotifyService.pushIfDueSoon(row);
    }

    @Override
    public void toggleDone(TaskItemToggleDoneReqVO request) {
        toggleDone(SecurityFrameworkUtils.requireLoginUserId(), request);
    }

    private void toggleDone(Long userId, TaskItemToggleDoneReqVO request) {
        TaskItemDO row = requireOwnedTask(request.getId(), userId);
        String nextStatus = Boolean.TRUE.equals(request.getDone()) ? TaskItemStatus.DONE : TaskItemStatus.TODO;
        if (Objects.equals(nextStatus, row.getStatus())) {
            return;
        }
        row.setStatus(nextStatus);
        row.setUpdater(userId);
        row.setUpdateTime(OffsetDateTime.now());
        taskItemMapper.updateById(row);
    }

    @Override
    public void deleteTask(Long id) {
        deleteTask(SecurityFrameworkUtils.requireLoginUserId(), id);
    }

    private void deleteTask(Long userId, Long id) {
        requireOwnedTask(id, userId);
        taskItemMapper.deleteById(id);
    }

    private TaskItemDO requireOwnedTask(Long id, Long userId) {
        TaskItemDO row = taskItemMapper.selectById(id);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_NOT_FOUND);
        }
        if (!Objects.equals(row.getAssigneeId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.TASK_FORBIDDEN);
        }
        return row;
    }

    private String normalizeStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (TaskItemStatus.TODO.equals(normalized) || TaskItemStatus.DONE.equals(normalized)) {
            return normalized;
        }
        return null;
    }
}
