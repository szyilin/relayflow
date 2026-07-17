package com.relayflow.module.task.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.task.controller.app.vo.TaskActivityRespVO;
import com.relayflow.module.task.service.collab.TaskCollabService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/task/activity")
public class TaskActivityController {

    private final TaskCollabService taskCollabService;

    @GetMapping("/feed")
    public CommonResult<List<TaskActivityRespVO>> feed(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return CommonResult.success(taskCollabService.listActivityFeed(limit));
    }
}
