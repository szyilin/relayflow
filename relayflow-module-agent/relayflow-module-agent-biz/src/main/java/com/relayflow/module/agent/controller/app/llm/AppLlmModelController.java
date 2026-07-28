package com.relayflow.module.agent.controller.app.llm;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.agent.controller.app.llm.vo.LlmEnabledModelRespVO;
import com.relayflow.module.agent.service.llm.LlmConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/agent/llm/model")
public class AppLlmModelController {

    private final LlmConnectionService llmConnectionService;

    @GetMapping("/list-enabled")
    public CommonResult<List<LlmEnabledModelRespVO>> listEnabled() {
        return CommonResult.success(llmConnectionService.listEnabledModels());
    }
}
