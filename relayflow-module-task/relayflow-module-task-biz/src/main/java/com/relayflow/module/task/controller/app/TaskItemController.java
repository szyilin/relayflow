package com.relayflow.module.task.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskSearchItemRespVO;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.service.item.TaskItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/task/item")
public class TaskItemController {

    private final TaskItemService taskItemService;

    @GetMapping("/page")
    public CommonResult<PageResult<TaskItemRespVO>> page(@Valid TaskItemPageReqVO request) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(taskItemService.pageMyTasks(loginUser.getUserId(), request));
    }

    @GetMapping("/search")
    public CommonResult<List<TaskSearchItemRespVO>> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        LoginUser loginUser = requireLoginUser();
        String trimmed = requireKeyword(keyword);
        return CommonResult.success(taskItemService.searchMyTasks(loginUser.getUserId(), trimmed, clampLimit(limit))
                .stream()
                .map(this::toSearchItem)
                .toList());
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
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(taskItemService.createTask(
                loginUser.getUserId(), loginUser.getTenantId(), request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody TaskItemUpdateReqVO request) {
        LoginUser loginUser = requireLoginUser();
        taskItemService.updateTask(loginUser.getUserId(), request);
        return CommonResult.success(true);
    }

    @PutMapping("/toggle-done")
    public CommonResult<Boolean> toggleDone(@Valid @RequestBody TaskItemToggleDoneReqVO request) {
        LoginUser loginUser = requireLoginUser();
        taskItemService.toggleDone(loginUser.getUserId(), request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        LoginUser loginUser = requireLoginUser();
        taskItemService.deleteTask(loginUser.getUserId(), id);
        return CommonResult.success(true);
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(com.relayflow.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
