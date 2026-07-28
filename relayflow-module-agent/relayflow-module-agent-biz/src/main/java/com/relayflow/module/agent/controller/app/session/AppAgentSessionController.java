package com.relayflow.module.agent.controller.app.session;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionCreateReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionMessageRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendRespVO;
import com.relayflow.module.agent.service.session.AgentSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/agent/session")
public class AppAgentSessionController {

    private final AgentSessionService agentSessionService;

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AgentSessionCreateReqVO request) {
        return CommonResult.success(agentSessionService.create(request));
    }

    @GetMapping("/list")
    public CommonResult<List<AgentSessionRespVO>> list() {
        return CommonResult.success(agentSessionService.listMine());
    }

    @GetMapping("/get")
    public CommonResult<AgentSessionRespVO> get(@RequestParam @NotNull Long id) {
        return CommonResult.success(agentSessionService.getMine(id));
    }

    @GetMapping("/messages")
    public CommonResult<List<AgentSessionMessageRespVO>> messages(@RequestParam @NotNull Long sessionId) {
        return CommonResult.success(agentSessionService.listMessages(sessionId));
    }

    @PostMapping("/send")
    public CommonResult<AgentSessionSendRespVO> send(@Valid @RequestBody AgentSessionSendReqVO request) {
        return CommonResult.success(agentSessionService.send(request));
    }
}
