package com.relayflow.module.task.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupMoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupRespVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupUpdateReqVO;
import com.relayflow.module.task.service.minegroup.TaskMineGroupService;
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
@RequestMapping("/app-api/task/mine-group")
public class TaskMineGroupController {

    private final TaskMineGroupService taskMineGroupService;

    @GetMapping("/list")
    public CommonResult<TaskMineGroupListRespVO> list() {
        return CommonResult.success(taskMineGroupService.list());
    }

    @PostMapping("/create")
    public CommonResult<TaskMineGroupRespVO> create(@Valid @RequestBody TaskMineGroupCreateReqVO request) {
        return CommonResult.success(taskMineGroupService.create(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody TaskMineGroupUpdateReqVO request) {
        taskMineGroupService.update(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        taskMineGroupService.delete(id);
        return CommonResult.success(true);
    }

    @PutMapping("/move")
    public CommonResult<Boolean> move(@Valid @RequestBody TaskMineGroupMoveReqVO request) {
        taskMineGroupService.move(request);
        return CommonResult.success(true);
    }
}
