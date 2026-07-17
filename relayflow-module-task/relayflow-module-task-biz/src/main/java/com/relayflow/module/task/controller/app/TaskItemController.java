package com.relayflow.module.task.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskSearchItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskSubtaskCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskActivityRespVO;
import com.relayflow.module.task.controller.app.vo.TaskAssignReqVO;
import com.relayflow.module.task.controller.app.vo.TaskCommentCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskCommentRespVO;
import com.relayflow.module.task.controller.app.vo.TaskFollowReqVO;
import com.relayflow.module.task.controller.app.vo.TaskFollowerRespVO;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.service.collab.TaskCollabService;
import com.relayflow.module.task.service.item.TaskItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/task/item")
public class TaskItemController {

    private final TaskItemService taskItemService;
    private final TaskCollabService taskCollabService;

    @GetMapping("/page")
    public CommonResult<PageResult<TaskItemRespVO>> page(@Valid TaskItemPageReqVO request) {
        return CommonResult.success(taskItemService.pageMyTasks(request));
    }

    @GetMapping("/search")
    public CommonResult<List<TaskSearchItemRespVO>> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        String trimmed = requireKeyword(keyword);
        return CommonResult.success(taskItemService.searchMyTasks(trimmed, clampLimit(limit))
                .stream()
                .map(this::toSearchItem)
                .toList());
    }

    @GetMapping("/due-range")
    public CommonResult<List<TaskItemRespVO>> dueRange(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "limit", defaultValue = "200") int limit) {
        return CommonResult.success(taskItemService.listDueRange(from, to, limit));
    }

    @GetMapping("/get")
    public CommonResult<TaskItemRespVO> get(@RequestParam @NotNull Long id) {
        return CommonResult.success(taskItemService.getTask(id));
    }

    @GetMapping("/subtasks")
    public CommonResult<List<TaskItemRespVO>> subtasks(@RequestParam @NotNull Long parentId) {
        return CommonResult.success(taskItemService.listSubtasks(parentId));
    }

    @PostMapping("/subtask/create")
    public CommonResult<Long> createSubtask(@Valid @RequestBody TaskSubtaskCreateReqVO request) {
        return CommonResult.success(taskItemService.createSubtask(request));
    }

    @PostMapping("/follow")
    public CommonResult<Boolean> follow(@Valid @RequestBody TaskFollowReqVO request) {
        taskCollabService.follow(request.getTaskId());
        return CommonResult.success(true);
    }

    @PostMapping("/unfollow")
    public CommonResult<Boolean> unfollow(@Valid @RequestBody TaskFollowReqVO request) {
        taskCollabService.unfollow(request.getTaskId());
        return CommonResult.success(true);
    }

    @GetMapping("/followers")
    public CommonResult<List<TaskFollowerRespVO>> followers(@RequestParam @NotNull Long taskId) {
        return CommonResult.success(taskCollabService.listFollowers(taskId));
    }

    @GetMapping("/following/page")
    public CommonResult<PageResult<TaskItemRespVO>> followingPage(@Valid TaskItemPageReqVO request) {
        return CommonResult.success(taskCollabService.pageFollowing(request));
    }

    @PutMapping("/assign")
    public CommonResult<Boolean> assign(@Valid @RequestBody TaskAssignReqVO request) {
        taskCollabService.assign(request);
        return CommonResult.success(true);
    }

    @GetMapping("/comments")
    public CommonResult<List<TaskCommentRespVO>> comments(@RequestParam @NotNull Long taskId) {
        return CommonResult.success(taskCollabService.listComments(taskId));
    }

    @PostMapping("/comment/create")
    public CommonResult<Long> createComment(@Valid @RequestBody TaskCommentCreateReqVO request) {
        return CommonResult.success(taskCollabService.createComment(request));
    }

    @GetMapping("/activities")
    public CommonResult<List<TaskActivityRespVO>> activities(@RequestParam @NotNull Long taskId) {
        return CommonResult.success(taskCollabService.listActivities(taskId));
    }

    private TaskSearchItemRespVO toSearchItem(TaskItemRespVO item) {
        TaskSearchItemRespVO vo = new TaskSearchItemRespVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setSubtitle(item.getStatus());
        vo.setRoute("/app/tasks?taskId=" + item.getId());
        vo.setEntityType("task");
        vo.setEntityId(String.valueOf(item.getId()));
        return vo;
    }

    private String requireKeyword(String keyword) {
        if (!StringUtils.hasText(keyword) || !StringUtils.hasText(keyword.trim())) {
            throw new ServiceException(ErrorCodeConstants.SEARCH_KEYWORD_REQUIRED);
        }
        return keyword.trim();
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 10);
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody TaskItemCreateReqVO request) {
        return CommonResult.success(taskItemService.createTask(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody TaskItemUpdateReqVO request) {
        taskItemService.updateTask(request);
        return CommonResult.success(true);
    }

    @PutMapping("/toggle-done")
    public CommonResult<Boolean> toggleDone(@Valid @RequestBody TaskItemToggleDoneReqVO request) {
        taskItemService.toggleDone(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        taskItemService.deleteTask(id);
        return CommonResult.success(true);
    }
}
