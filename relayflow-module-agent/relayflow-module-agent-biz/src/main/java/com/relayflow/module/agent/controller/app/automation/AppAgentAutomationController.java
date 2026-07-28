package com.relayflow.module.agent.controller.app.automation;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.agent.controller.app.automation.vo.AgentAutomationRespVO;
import com.relayflow.module.agent.controller.app.automation.vo.AgentAutomationSaveReqVO;
import com.relayflow.module.agent.service.automation.AgentAutomationService;
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

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/agent/automation")
public class AppAgentAutomationController {

    private final AgentAutomationService agentAutomationService;

    @GetMapping("/list")
    public CommonResult<List<AgentAutomationRespVO>> list() {
        return CommonResult.success(agentAutomationService.listMine());
    }

    @GetMapping("/get")
    public CommonResult<AgentAutomationRespVO> get(@RequestParam @NotNull Long id) {
        return CommonResult.success(agentAutomationService.getMine(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AgentAutomationSaveReqVO request) {
        return CommonResult.success(agentAutomationService.create(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody AgentAutomationSaveReqVO request) {
        agentAutomationService.update(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        agentAutomationService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/run-now")
    public CommonResult<Boolean> runNow(@RequestParam @NotNull Long id) {
        agentAutomationService.runNow(id);
        return CommonResult.success(true);
    }
}
