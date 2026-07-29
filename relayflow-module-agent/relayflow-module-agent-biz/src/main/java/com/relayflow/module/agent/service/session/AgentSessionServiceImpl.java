package com.relayflow.module.agent.service.session;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.builder.AgentBuilder;
import com.relayflow.module.agent.builder.RuntimeAgent;
import com.relayflow.module.agent.channel.AgentChannel;
import com.relayflow.module.agent.controller.app.session.vo.AgentMessagePartVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionCreateReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionMessageRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendRespVO;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import com.relayflow.module.agent.dal.dataobject.AgentSessionDO;
import com.relayflow.module.agent.dal.dataobject.AgentSessionMessageDO;
import com.relayflow.module.agent.dal.mapper.AgentDefMapper;
import com.relayflow.module.agent.dal.mapper.AgentSessionMapper;
import com.relayflow.module.agent.dal.mapper.AgentSessionMessageMapper;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.runtime.AgentRunCommand;
import com.relayflow.module.agent.runtime.AgentRunResult;
import com.relayflow.module.agent.runtime.AgentRuntime;
import com.relayflow.module.agent.runtime.AgentStreamEvent;
import com.relayflow.module.agent.skill.assembly.CapabilityAssembly;
import com.relayflow.module.agent.skill.assembly.CapabilityAssemblyService;
import com.relayflow.module.agent.skill.assembly.CapabilityAuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSessionServiceImpl implements AgentSessionService {

    private static final String STATUS_ACTIVE = "active";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String AGENT_STATUS_ACTIVE = "active";

    private final AgentSessionMapper sessionMapper;
    private final AgentSessionMessageMapper messageMapper;
    private final AgentDefMapper agentDefMapper;
    private final AgentBuilder agentBuilder;
    private final AgentRuntime agentRuntime;
    private final CapabilityAssemblyService capabilityAssemblyService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public Long create(AgentSessionCreateReqVO request) {
        AgentDefDO agent = requireAccessibleAgent(request.getAgentId());
        if (!AGENT_STATUS_ACTIVE.equals(agent.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_DISABLED);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();

        AgentSessionDO session = new AgentSessionDO();
        session.setTenantId(tenantId);
        session.setAgentId(agent.getId());
        session.setUserId(userId);
        session.setStatus(STATUS_ACTIVE);
        if (StringUtils.hasText(request.getTitle())) {
            session.setTitle(request.getTitle().trim());
        }
        sessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public List<AgentSessionRespVO> listMine() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        List<AgentSessionDO> sessions = sessionMapper.selectList(Wrappers.<AgentSessionDO>lambdaQuery()
                .eq(AgentSessionDO::getTenantId, tenantId)
                .eq(AgentSessionDO::getUserId, userId)
                .orderByDesc(AgentSessionDO::getUpdateTime));
        if (sessions.isEmpty()) {
            return List.of();
        }
        List<Long> agentIds = sessions.stream().map(AgentSessionDO::getAgentId).distinct().toList();
        Map<Long, AgentDefDO> agents = agentDefMapper.selectList(Wrappers.<AgentDefDO>lambdaQuery()
                        .eq(AgentDefDO::getTenantId, tenantId)
                        .in(AgentDefDO::getId, agentIds))
                .stream()
                .collect(Collectors.toMap(AgentDefDO::getId, Function.identity(), (a, b) -> a));
        return sessions.stream().map(s -> toSessionVo(s, agents.get(s.getAgentId()))).toList();
    }

    @Override
    public AgentSessionRespVO getMine(Long id) {
        AgentSessionDO session = requireOwnedSession(id);
        AgentDefDO agent = agentDefMapper.selectById(session.getAgentId());
        return toSessionVo(session, agent);
    }

    @Override
    public List<AgentSessionMessageRespVO> listMessages(Long sessionId) {
        requireOwnedSession(sessionId);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return messageMapper.selectList(Wrappers.<AgentSessionMessageDO>lambdaQuery()
                        .eq(AgentSessionMessageDO::getTenantId, tenantId)
                        .eq(AgentSessionMessageDO::getSessionId, sessionId)
                        .orderByAsc(AgentSessionMessageDO::getSeq))
                .stream()
                .map(this::toMessageVo)
                .toList();
    }

    @Override
    @Transactional
    public AgentSessionRespVO switchAgent(Long sessionId, Long agentId) {
        AgentSessionDO session = requireOwnedSession(sessionId);
        AgentDefDO agent = requireAccessibleAgent(agentId);
        if (!AGENT_STATUS_ACTIVE.equals(agent.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_DISABLED);
        }
        session.setAgentId(agent.getId());
        session.setUpdateTime(OffsetDateTime.now());
        sessionMapper.updateById(session);
        return toSessionVo(session, agent);
    }

    @Override
    @Transactional
    public AgentSessionSendRespVO send(AgentSessionSendReqVO request) {
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_MESSAGE_EMPTY);
        }
        AgentSessionDO session = requireOwnedSession(request.getSessionId());
        AgentDefDO agent = requireAccessibleAgent(session.getAgentId());
        if (!AGENT_STATUS_ACTIVE.equals(agent.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_DISABLED);
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<AgentSessionMessageDO> existing = messageMapper.selectList(Wrappers.<AgentSessionMessageDO>lambdaQuery()
                .eq(AgentSessionMessageDO::getTenantId, tenantId)
                .eq(AgentSessionMessageDO::getSessionId, session.getId())
                .orderByAsc(AgentSessionMessageDO::getSeq));

        int nextSeq = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getSeq() + 1;
        String userText = request.getContent().trim();

        AgentSessionMessageDO userRow = new AgentSessionMessageDO();
        userRow.setTenantId(tenantId);
        userRow.setSessionId(session.getId());
        userRow.setRole(ROLE_USER);
        userRow.setContent(userText);
        userRow.setPartsJson(writePartsJson(List.of(textPart(userText))));
        userRow.setSeq(nextSeq);
        messageMapper.insert(userRow);

        List<Message> history = toSpringHistory(existing);
        CapabilityAssembly assembly = capabilityAssemblyService.assemble(agent, AgentChannel.AGENT_WEB);
        CapabilityAuthContext.set(new CapabilityAuthContext.CapabilityAuthScope(
                java.util.Set.copyOf(assembly.getDeferredDomains()),
                assembly.getHotSkillCodeSet(),
                assembly.getDeferredSkillCodeSet()));
        try {
            RuntimeAgent runtimeAgent = agentBuilder.build(
                    agent, assembly.getTools(), assembly.getDomainSummaryAppendix());
            AgentRunResult result = agentRuntime.run(AgentRunCommand.builder()
                    .modelRef(runtimeAgent.getModelRef())
                    .systemPrompt(runtimeAgent.getSystemPrompt())
                    .history(history)
                    .userMessage(userText)
                    .tools(runtimeAgent.getTools())
                    .build());

            String assistantText = result.getAssistantText() == null ? "" : result.getAssistantText();
            AgentSessionMessageDO assistantRow = new AgentSessionMessageDO();
            assistantRow.setTenantId(tenantId);
            assistantRow.setSessionId(session.getId());
            assistantRow.setRole(ROLE_ASSISTANT);
            assistantRow.setContent(assistantText);
            assistantRow.setPartsJson(writePartsJson(List.of(textPart(assistantText))));
            assistantRow.setSeq(nextSeq + 1);
            messageMapper.insert(assistantRow);

            touchSessionTitle(session, userText);

            AgentSessionSendRespVO resp = new AgentSessionSendRespVO();
            resp.setUserMessage(toMessageVo(userRow));
            resp.setAssistantMessage(toMessageVo(assistantRow));
            return resp;
        } finally {
            CapabilityAuthContext.clear();
        }
    }

    @Override
    public void sendStream(AgentSessionSendReqVO request, Consumer<Map<String, Object>> onEvent, AtomicBoolean cancelFlag) {
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_MESSAGE_EMPTY);
        }
        Consumer<Map<String, Object>> sink = onEvent == null ? e -> {
        } : onEvent;
        AtomicBoolean cancel = cancelFlag == null ? new AtomicBoolean(false) : cancelFlag;

        StreamPrep prep = transactionTemplate.execute(status -> prepareStreamTurn(request));
        if (prep == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_RUNTIME_FAILED.getCode(), "无法准备流式会话");
        }

        sink.accept(messageEvent("message.user", prep.userVo()));

        List<AgentMessagePartVO> assistantParts = new ArrayList<>();
        StringBuilder textBuf = new StringBuilder();

        try {
            String finalText = agentRuntime.stream(prep.command(), event -> {
                if (cancel.get()) {
                    return;
                }
                applyRuntimeEvent(event, assistantParts, textBuf, sink);
            }, cancel);

            if (cancel.get()) {
                return;
            }

            String assistantText = StringUtils.hasText(finalText) ? finalText : textBuf.toString();
            if (assistantParts.stream().noneMatch(p -> "text".equals(p.getType()))) {
                assistantParts.add(textPart(assistantText));
            } else {
                for (AgentMessagePartVO part : assistantParts) {
                    if ("text".equals(part.getType())) {
                        part.setText(assistantText);
                    }
                }
            }

            AgentSessionMessageRespVO assistantVo = transactionTemplate.execute(status ->
                    persistAssistant(prep, assistantText, assistantParts));
            if (assistantVo != null) {
                sink.accept(messageEvent("message.assistant", assistantVo));
            }
            sink.accept(Map.of("type", "done"));
        } catch (ServiceException ex) {
            sink.accept(errorEvent(ex.getMessage(), ex.getCode()));
            throw ex;
        } catch (Exception ex) {
            sink.accept(errorEvent("智能体运行失败：" + ex.getMessage(),
                    ErrorCodeConstants.AGENT_RUNTIME_FAILED.getCode()));
            throw new ServiceException(ErrorCodeConstants.AGENT_RUNTIME_FAILED.getCode(),
                    "智能体运行失败：" + ex.getMessage());
        } finally {
            CapabilityAuthContext.clear();
        }
    }

    private StreamPrep prepareStreamTurn(AgentSessionSendReqVO request) {
        AgentSessionDO session = requireOwnedSession(request.getSessionId());
        AgentDefDO agent = requireAccessibleAgent(session.getAgentId());
        if (!AGENT_STATUS_ACTIVE.equals(agent.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_DISABLED);
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<AgentSessionMessageDO> existing = messageMapper.selectList(Wrappers.<AgentSessionMessageDO>lambdaQuery()
                .eq(AgentSessionMessageDO::getTenantId, tenantId)
                .eq(AgentSessionMessageDO::getSessionId, session.getId())
                .orderByAsc(AgentSessionMessageDO::getSeq));

        int nextSeq = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getSeq() + 1;
        String userText = request.getContent().trim();

        AgentSessionMessageDO userRow = new AgentSessionMessageDO();
        userRow.setTenantId(tenantId);
        userRow.setSessionId(session.getId());
        userRow.setRole(ROLE_USER);
        userRow.setContent(userText);
        userRow.setPartsJson(writePartsJson(List.of(textPart(userText))));
        userRow.setSeq(nextSeq);
        messageMapper.insert(userRow);

        List<Message> history = toSpringHistory(existing);
        CapabilityAssembly assembly = capabilityAssemblyService.assemble(agent, AgentChannel.AGENT_WEB);
        CapabilityAuthContext.set(new CapabilityAuthContext.CapabilityAuthScope(
                java.util.Set.copyOf(assembly.getDeferredDomains()),
                assembly.getHotSkillCodeSet(),
                assembly.getDeferredSkillCodeSet()));
        RuntimeAgent runtimeAgent = agentBuilder.build(
                agent, assembly.getTools(), assembly.getDomainSummaryAppendix());
        AgentRunCommand command = AgentRunCommand.builder()
                .modelRef(runtimeAgent.getModelRef())
                .systemPrompt(runtimeAgent.getSystemPrompt())
                .history(history)
                .userMessage(userText)
                .tools(runtimeAgent.getTools())
                .build();

        return new StreamPrep(session.getId(), tenantId, nextSeq + 1, userText, toMessageVo(userRow), command);
    }

    private AgentSessionMessageRespVO persistAssistant(StreamPrep prep, String assistantText, List<AgentMessagePartVO> parts) {
        AgentSessionMessageDO assistantRow = new AgentSessionMessageDO();
        assistantRow.setTenantId(prep.tenantId());
        assistantRow.setSessionId(prep.sessionId());
        assistantRow.setRole(ROLE_ASSISTANT);
        assistantRow.setContent(assistantText == null ? "" : assistantText);
        assistantRow.setPartsJson(writePartsJson(parts));
        assistantRow.setSeq(prep.assistantSeq());
        messageMapper.insert(assistantRow);

        AgentSessionDO session = sessionMapper.selectById(prep.sessionId());
        if (session != null) {
            touchSessionTitle(session, prep.userText());
        }
        return toMessageVo(assistantRow);
    }

    private void applyRuntimeEvent(
            AgentStreamEvent event,
            List<AgentMessagePartVO> parts,
            StringBuilder textBuf,
            Consumer<Map<String, Object>> sink
    ) {
        if (event == null || event.getType() == null) {
            return;
        }
        switch (event.getType()) {
            case AgentStreamEvent.REASONING_DELTA -> {
                AgentMessagePartVO last = lastPart(parts, "reasoning");
                if (last == null) {
                    AgentMessagePartVO part = new AgentMessagePartVO();
                    part.setType("reasoning");
                    part.setText(event.getText() == null ? "" : event.getText());
                    parts.add(part);
                } else {
                    last.setText((last.getText() == null ? "" : last.getText()) + nullToEmpty(event.getText()));
                }
                sink.accept(eventMap(event));
            }
            case AgentStreamEvent.REASONING_END -> sink.accept(eventMap(event));
            case AgentStreamEvent.TOOL_START -> {
                AgentMessagePartVO part = new AgentMessagePartVO();
                part.setType("tool");
                part.setToolCallId(event.getToolCallId());
                part.setToolName(event.getToolName());
                part.setInput(event.getInput());
                parts.add(part);
                sink.accept(eventMap(event));
            }
            case AgentStreamEvent.TOOL_RESULT -> {
                for (int i = parts.size() - 1; i >= 0; i--) {
                    AgentMessagePartVO part = parts.get(i);
                    if ("tool".equals(part.getType())
                            && Objects.equals(part.getToolName(), event.getToolName())
                            && part.getOutput() == null) {
                        part.setOutput(event.getOutput());
                        if (event.getToolCallId() != null) {
                            part.setToolCallId(event.getToolCallId());
                        }
                        break;
                    }
                }
                sink.accept(eventMap(event));
            }
            case AgentStreamEvent.TEXT_DELTA -> {
                textBuf.append(nullToEmpty(event.getText()));
                AgentMessagePartVO last = lastPart(parts, "text");
                if (last == null) {
                    parts.add(textPart(nullToEmpty(event.getText())));
                } else {
                    last.setText((last.getText() == null ? "" : last.getText()) + nullToEmpty(event.getText()));
                }
                sink.accept(eventMap(event));
            }
            case AgentStreamEvent.DONE -> {
                // session layer emits contract done after persist
            }
            case AgentStreamEvent.ERROR -> sink.accept(eventMap(event));
            default -> log.debug("忽略未知 stream 事件: {}", event.getType());
        }
    }

    private static AgentMessagePartVO lastPart(List<AgentMessagePartVO> parts, String type) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            AgentMessagePartVO part = parts.get(i);
            if (type.equals(part.getType())) {
                return part;
            }
        }
        return null;
    }

    private Map<String, Object> eventMap(AgentStreamEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", event.getType());
        if (event.getText() != null) {
            map.put("text", event.getText());
        }
        if (event.getToolCallId() != null) {
            map.put("toolCallId", event.getToolCallId());
        }
        if (event.getToolName() != null) {
            map.put("toolName", event.getToolName());
        }
        if (event.getInput() != null) {
            map.put("input", event.getInput());
        }
        if (event.getOutput() != null) {
            map.put("output", event.getOutput());
        }
        if (event.getMessage() != null) {
            map.put("message", event.getMessage());
        }
        if (event.getCode() != null) {
            map.put("code", event.getCode());
        }
        return map;
    }

    private Map<String, Object> messageEvent(String type, AgentSessionMessageRespVO vo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("id", vo.getId() == null ? null : String.valueOf(vo.getId()));
        map.put("role", vo.getRole());
        map.put("seq", vo.getSeq());
        map.put("content", vo.getContent());
        if (vo.getParts() != null) {
            map.put("parts", vo.getParts());
        }
        return map;
    }

    private Map<String, Object> errorEvent(String message, Integer code) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "error");
        map.put("message", message);
        if (code != null) {
            map.put("code", code);
        }
        return map;
    }

    private void touchSessionTitle(AgentSessionDO session, String userText) {
        if (!StringUtils.hasText(session.getTitle())) {
            session.setTitle(truncateTitle(userText));
        }
        session.setUpdateTime(OffsetDateTime.now());
        sessionMapper.updateById(session);
    }

    private List<Message> toSpringHistory(List<AgentSessionMessageDO> rows) {
        List<Message> messages = new ArrayList<>();
        for (AgentSessionMessageDO row : rows) {
            if (ROLE_USER.equals(row.getRole())) {
                messages.add(new UserMessage(row.getContent()));
            } else if (ROLE_ASSISTANT.equals(row.getRole())) {
                messages.add(new AssistantMessage(row.getContent()));
            }
        }
        return messages;
    }

    private AgentDefDO requireAccessibleAgent(Long agentId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        AgentDefDO agent = agentDefMapper.selectOne(Wrappers.<AgentDefDO>lambdaQuery()
                .eq(AgentDefDO::getTenantId, tenantId)
                .eq(AgentDefDO::getId, agentId));
        if (agent == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_NOT_FOUND);
        }
        if (!Objects.equals(userId, agent.getOwnerUserId())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_FORBIDDEN);
        }
        return agent;
    }

    private AgentSessionDO requireOwnedSession(Long sessionId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        AgentSessionDO session = sessionMapper.selectOne(Wrappers.<AgentSessionDO>lambdaQuery()
                .eq(AgentSessionDO::getTenantId, tenantId)
                .eq(AgentSessionDO::getId, sessionId));
        if (session == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_SESSION_NOT_FOUND);
        }
        if (!Objects.equals(userId, session.getUserId())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_SESSION_FORBIDDEN);
        }
        return session;
    }

    private AgentSessionRespVO toSessionVo(AgentSessionDO session, AgentDefDO agent) {
        AgentSessionRespVO vo = new AgentSessionRespVO();
        vo.setId(session.getId());
        vo.setAgentId(session.getAgentId());
        vo.setAgentName(agent != null ? agent.getName() : null);
        vo.setTitle(session.getTitle());
        vo.setStatus(session.getStatus());
        if (session.getUpdateTime() != null) {
            vo.setUpdateTime(session.getUpdateTime().toString());
        } else if (session.getCreateTime() != null) {
            vo.setUpdateTime(session.getCreateTime().toString());
        }
        return vo;
    }

    private AgentSessionMessageRespVO toMessageVo(AgentSessionMessageDO row) {
        AgentSessionMessageRespVO vo = new AgentSessionMessageRespVO();
        vo.setId(row.getId());
        vo.setRole(row.getRole());
        vo.setContent(row.getContent());
        vo.setSeq(row.getSeq());
        vo.setParts(readParts(row.getPartsJson()));
        return vo;
    }

    private List<AgentMessagePartVO> readParts(String partsJson) {
        if (!StringUtils.hasText(partsJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(partsJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("解析 parts_json 失败: {}", ex.getMessage());
            return null;
        }
    }

    private String writePartsJson(List<AgentMessagePartVO> parts) {
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(parts);
        } catch (Exception ex) {
            log.warn("序列化 parts_json 失败: {}", ex.getMessage());
            return null;
        }
    }

    private static AgentMessagePartVO textPart(String text) {
        AgentMessagePartVO part = new AgentMessagePartVO();
        part.setType("text");
        part.setText(text == null ? "" : text);
        return part;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String truncateTitle(String text) {
        String compact = text.replace('\n', ' ').trim();
        if (compact.length() <= 40) {
            return compact;
        }
        return compact.substring(0, 40) + "…";
    }

    private record StreamPrep(
            Long sessionId,
            Long tenantId,
            int assistantSeq,
            String userText,
            AgentSessionMessageRespVO userVo,
            AgentRunCommand command
    ) {
    }
}
