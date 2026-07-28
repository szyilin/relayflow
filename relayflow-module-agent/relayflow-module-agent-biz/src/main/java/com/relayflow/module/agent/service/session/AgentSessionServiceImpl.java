package com.relayflow.module.agent.service.session;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.builder.AgentBuilder;
import com.relayflow.module.agent.builder.RuntimeAgent;
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
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        userRow.setSeq(nextSeq);
        messageMapper.insert(userRow);

        List<Message> history = toSpringHistory(existing);
        RuntimeAgent runtimeAgent = agentBuilder.build(agent, List.of());
        AgentRunResult result = agentRuntime.run(AgentRunCommand.builder()
                .modelRef(runtimeAgent.getModelRef())
                .systemPrompt(runtimeAgent.getSystemPrompt())
                .history(history)
                .userMessage(userText)
                .tools(runtimeAgent.getTools())
                .build());

        AgentSessionMessageDO assistantRow = new AgentSessionMessageDO();
        assistantRow.setTenantId(tenantId);
        assistantRow.setSessionId(session.getId());
        assistantRow.setRole(ROLE_ASSISTANT);
        assistantRow.setContent(result.getAssistantText() == null ? "" : result.getAssistantText());
        assistantRow.setSeq(nextSeq + 1);
        messageMapper.insert(assistantRow);

        if (!StringUtils.hasText(session.getTitle())) {
            session.setTitle(truncateTitle(userText));
        }
        session.setUpdateTime(OffsetDateTime.now());
        sessionMapper.updateById(session);

        AgentSessionSendRespVO resp = new AgentSessionSendRespVO();
        resp.setUserMessage(toMessageVo(userRow));
        resp.setAssistantMessage(toMessageVo(assistantRow));
        return resp;
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
        return vo;
    }

    private AgentSessionMessageRespVO toMessageVo(AgentSessionMessageDO row) {
        AgentSessionMessageRespVO vo = new AgentSessionMessageRespVO();
        vo.setId(row.getId());
        vo.setRole(row.getRole());
        vo.setContent(row.getContent());
        vo.setSeq(row.getSeq());
        return vo;
    }

    private static String truncateTitle(String text) {
        String compact = text.replace('\n', ' ').trim();
        if (compact.length() <= 40) {
            return compact;
        }
        return compact.substring(0, 40) + "…";
    }
}
