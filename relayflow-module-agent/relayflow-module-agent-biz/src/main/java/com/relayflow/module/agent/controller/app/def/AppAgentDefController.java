package com.relayflow.module.agent.controller.app.def;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.agent.controller.app.def.vo.AgentDefRespVO;
import com.relayflow.module.agent.controller.app.def.vo.AgentDefSaveReqVO;
import com.relayflow.module.agent.service.definition.AgentDefService;
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
@RequestMapping("/app-api/agent/def")
public class AppAgentDefController {

    private final AgentDefService agentDefService;

    @GetMapping("/list")
    public CommonResult<List<AgentDefRespVO>> list() {
        return CommonResult.success(agentDefService.listMine());
    }

    @GetMapping("/get")
    public CommonResult<AgentDefRespVO> get(@RequestParam @NotNull Long id) {
        return CommonResult.success(agentDefService.getMine(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AgentDefSaveReqVO request) {
        return CommonResult.success(agentDefService.create(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody AgentDefSaveReqVO request) {
        agentDefService.update(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        agentDefService.delete(id);
        return CommonResult.success(true);
    }
}
