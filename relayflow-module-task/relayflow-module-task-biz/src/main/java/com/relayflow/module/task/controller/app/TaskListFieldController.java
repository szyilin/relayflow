package com.relayflow.module.task.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.task.controller.app.vo.TaskListFieldCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldValuePutReqVO;
import com.relayflow.module.task.service.listfield.TaskListFieldService;
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
@RequestMapping("/app-api/task/list-field")
public class TaskListFieldController {

    private final TaskListFieldService taskListFieldService;

    @GetMapping("/list")
    public CommonResult<TaskListFieldListRespVO> list(@RequestParam @NotNull Long listId) {
        return CommonResult.success(taskListFieldService.list(listId));
    }

    @PostMapping("/create")
    public CommonResult<TaskListFieldRespVO> create(@Valid @RequestBody TaskListFieldCreateReqVO request) {
        return CommonResult.success(taskListFieldService.create(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody TaskListFieldUpdateReqVO request) {
        taskListFieldService.update(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        taskListFieldService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/option/create")
    public CommonResult<TaskListFieldOptionRespVO> createOption(
            @Valid @RequestBody TaskListFieldOptionCreateReqVO request) {
        return CommonResult.success(taskListFieldService.createOption(request));
    }

    @PutMapping("/option/update")
    public CommonResult<Boolean> updateOption(@Valid @RequestBody TaskListFieldOptionUpdateReqVO request) {
        taskListFieldService.updateOption(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/option/delete")
    public CommonResult<Boolean> deleteOption(@RequestParam @NotNull Long id) {
        taskListFieldService.deleteOption(id);
        return CommonResult.success(true);
    }

    @PutMapping("/value")
    public CommonResult<Boolean> putValue(@Valid @RequestBody TaskListFieldValuePutReqVO request) {
        taskListFieldService.putValue(request);
        return CommonResult.success(true);
    }
}
