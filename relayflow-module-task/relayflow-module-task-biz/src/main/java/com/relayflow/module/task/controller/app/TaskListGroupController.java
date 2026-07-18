package com.relayflow.module.task.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.task.controller.app.vo.TaskListGroupCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupMoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupUpdateReqVO;
import com.relayflow.module.task.service.listgroup.TaskListGroupService;
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
@RequestMapping("/app-api/task/list-group")
public class TaskListGroupController {

    private final TaskListGroupService taskListGroupService;

    @GetMapping("/list")
    public CommonResult<TaskListGroupListRespVO> list(@RequestParam @NotNull Long listId) {
        return CommonResult.success(taskListGroupService.list(listId));
    }

    @PostMapping("/create")
    public CommonResult<TaskListGroupRespVO> create(@Valid @RequestBody TaskListGroupCreateReqVO request) {
        return CommonResult.success(taskListGroupService.create(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody TaskListGroupUpdateReqVO request) {
        taskListGroupService.update(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        taskListGroupService.delete(id);
        return CommonResult.success(true);
    }

    @PutMapping("/move")
    public CommonResult<Boolean> move(@Valid @RequestBody TaskListGroupMoveReqVO request) {
        taskListGroupService.move(request);
        return CommonResult.success(true);
    }
}
