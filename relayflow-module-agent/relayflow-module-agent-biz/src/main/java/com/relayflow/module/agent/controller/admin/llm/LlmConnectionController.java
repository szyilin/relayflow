package com.relayflow.module.agent.controller.admin.llm;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionRespVO;
import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionSaveReqVO;
import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionTestReqVO;
import com.relayflow.module.agent.service.llm.LlmConnectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/admin-api/agent/llm/connection")
public class LlmConnectionController {

    private final LlmConnectionService llmConnectionService;

    @PreAuthorize("hasAuthority('agent:llm:query')")
    @GetMapping("/list")
    public CommonResult<List<LlmConnectionRespVO>> list() {
        return CommonResult.success(llmConnectionService.list());
    }

    @PreAuthorize("hasAuthority('agent:llm:query')")
    @GetMapping("/get")
    public CommonResult<LlmConnectionRespVO> get(@RequestParam @NotNull Long id) {
        return CommonResult.success(llmConnectionService.get(id));
    }

    @PreAuthorize("hasAuthority('agent:llm:update')")
    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody LlmConnectionSaveReqVO request) {
        return CommonResult.success(llmConnectionService.create(request));
    }

    @PreAuthorize("hasAuthority('agent:llm:update')")
    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody LlmConnectionSaveReqVO request) {
        llmConnectionService.update(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('agent:llm:update')")
    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        llmConnectionService.delete(id);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('agent:llm:test')")
    @PostMapping("/test")
    public CommonResult<Boolean> test(@Valid @RequestBody LlmConnectionTestReqVO request) {
        llmConnectionService.testConnection(request);
        return CommonResult.success(true);
    }
}
