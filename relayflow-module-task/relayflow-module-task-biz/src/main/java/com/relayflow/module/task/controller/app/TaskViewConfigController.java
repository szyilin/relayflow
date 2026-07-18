package com.relayflow.module.task.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.task.controller.app.vo.TaskViewConfigSaveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskViewConfigVO;
import com.relayflow.module.task.service.viewconfig.TaskViewConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/task/view-config")
public class TaskViewConfigController {

    private final TaskViewConfigService taskViewConfigService;

    @GetMapping("/get")
    public CommonResult<TaskViewConfigVO> get(
            @RequestParam @NotBlank String contextType,
            @RequestParam(required = false) Long contextId) {
        return CommonResult.success(taskViewConfigService.get(contextType, contextId));
    }

    @PutMapping("/save")
    public CommonResult<Boolean> save(@Valid @RequestBody TaskViewConfigSaveReqVO request) {
        taskViewConfigService.save(request);
        return CommonResult.success(true);
    }
}
