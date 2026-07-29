package com.relayflow.module.agent.controller.app.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionCreateReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionMessageRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSwitchAgentReqVO;
import com.relayflow.module.agent.service.session.AgentSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/agent/session")
public class AppAgentSessionController {

    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

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

    @PostMapping("/switch-agent")
    public CommonResult<AgentSessionRespVO> switchAgent(@Valid @RequestBody AgentSessionSwitchAgentReqVO request) {
        return CommonResult.success(agentSessionService.switchAgent(request.getSessionId(), request.getAgentId()));
    }

    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendStream(@Valid @RequestBody AgentSessionSendReqVO request) {
        // Capture auth before async handoff
        SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        SecurityContext securityContext = SecurityContextHolder.getContext();

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean cancel = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancel.set(true));
        emitter.onTimeout(() -> cancel.set(true));
        emitter.onError(ex -> cancel.set(true));

        Thread worker = new Thread(() -> {
            SecurityContextHolder.setContext(securityContext);
            TenantContextHolder.set(tenantId);
            try {
                agentSessionService.sendStream(request, event -> sendEvent(emitter, event, cancel), cancel);
                if (!cancel.get()) {
                    emitter.complete();
                }
            } catch (ServiceException ex) {
                try {
                    if (!cancel.get()) {
                        sendEvent(emitter, Map.of(
                                "type", "error",
                                "message", ex.getMessage() == null ? "error" : ex.getMessage(),
                                "code", ex.getCode() == null ? 0 : ex.getCode()
                        ), cancel);
                        emitter.complete();
                    }
                } catch (Exception ignored) {
                    emitter.completeWithError(ex);
                }
            } catch (Exception ex) {
                log.warn("agent send/stream failed: {}", ex.getMessage());
                emitter.completeWithError(ex);
            } finally {
                TenantContextHolder.clear();
                SecurityContextHolder.clearContext();
            }
        }, "agent-session-stream");
        worker.setDaemon(true);
        worker.start();
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, Map<String, Object> event, AtomicBoolean cancel) {
        if (cancel.get()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event().data(json, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            cancel.set(true);
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignored) {
                // already completed
            }
        }
    }
}
