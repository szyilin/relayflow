package com.relayflow.module.task.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.service.item.TaskItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
