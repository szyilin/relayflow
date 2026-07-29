package com.relayflow.module.agent.api.carrier;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.api.carrier.dto.AgentCarrierHistoryMessageDTO;
import com.relayflow.module.agent.api.carrier.dto.AgentCarrierInboundCommand;
import com.relayflow.module.agent.builder.AgentBuilder;
import com.relayflow.module.agent.builder.RuntimeAgent;
import com.relayflow.module.agent.channel.AgentChannel;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import com.relayflow.module.agent.dal.mapper.AgentDefMapper;
import com.relayflow.module.agent.runtime.AgentRunCommand;
import com.relayflow.module.agent.runtime.AgentRunResult;
import com.relayflow.module.agent.runtime.AgentRuntime;
import com.relayflow.module.agent.skill.assembly.CapabilityAssembly;
import com.relayflow.module.agent.skill.assembly.CapabilityAssemblyService;
import com.relayflow.module.agent.skill.assembly.CapabilityAuthContext;
import com.relayflow.module.agent.skill.im.ImCarrierToolContext;
import com.relayflow.module.agent.skill.im.ImCarrierToolScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * IM channel adapter: runs AgentRuntime without writing agent_session*.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCarrierInboundApiImpl implements AgentCarrierInboundApi {

    private static final String STATUS_ACTIVE = "active";

    private final AgentDefMapper agentDefMapper;
    private final AgentBuilder agentBuilder;
    private final AgentRuntime agentRuntime;
    private final CapabilityAssemblyService capabilityAssemblyService;

    @Override
    public String handleInbound(AgentCarrierInboundCommand command) {
        if (command == null || command.getTenantId() == null || command.getAgentId() == null
                || !StringUtils.hasText(command.getInboundText())) {
            return null;
        }
        Long previousTenant = TenantContextHolder.get();
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            TenantContextHolder.set(command.getTenantId());
            if (command.getTriggerUserId() != null) {
                LoginUser loginUser = new LoginUser(
                        command.getTriggerUserId(), "carrier", command.getTenantId(), "user", List.of());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(
                        loginUser, null, loginUser.getAuthorities()));
                SecurityContextHolder.setContext(context);
            }
            ImCarrierToolContext.set(new ImCarrierToolScope(
                    command.getTenantId(),
                    command.getConversationId(),
                    command.getTriggerUserId(),
                    command.getBotId()));

            AgentDefDO agent = agentDefMapper.selectOne(Wrappers.<AgentDefDO>lambdaQuery()
                    .eq(AgentDefDO::getTenantId, command.getTenantId())
                    .eq(AgentDefDO::getId, command.getAgentId()));
            if (agent == null || !STATUS_ACTIVE.equals(agent.getStatus())) {
                return null;
            }
            CapabilityAssembly assembly = capabilityAssemblyService.assemble(agent, AgentChannel.IM_BOT);
            CapabilityAuthContext.set(new CapabilityAuthContext.CapabilityAuthScope(
                    Set.copyOf(assembly.getDeferredDomains()),
                    assembly.getHotSkillCodeSet(),
                    assembly.getDeferredSkillCodeSet()));
            RuntimeAgent runtimeAgent = agentBuilder.build(
                    agent, assembly.getTools(), assembly.getDomainSummaryAppendix());
            String systemPrompt = appendImToolHint(runtimeAgent.getSystemPrompt());
            AgentRunResult result = agentRuntime.run(AgentRunCommand.builder()
                    .modelRef(runtimeAgent.getModelRef())
                    .systemPrompt(systemPrompt)
                    .history(toSpringHistory(command.getHistory()))
                    .userMessage(command.getInboundText().trim())
                    .tools(runtimeAgent.getTools())
                    .build());
            String text = result.getAssistantText();
            return StringUtils.hasText(text) ? text : null;
        } catch (Exception ex) {
            log.warn("AgentCarrierInbound failed: agentId={}, conversationId={}",
                    command.getAgentId(), command.getConversationId(), ex);
            String msg = ex.getMessage();
            return StringUtils.hasText(msg) ? "智能体暂时无法回复：" + msg : "智能体暂时无法回复，请稍后重试";
        } finally {
            CapabilityAuthContext.clear();
            ImCarrierToolContext.clear();
            if (previousTenant != null) {
                TenantContextHolder.set(previousTenant);
            } else {
                TenantContextHolder.clear();
            }
            SecurityContextHolder.setContext(previousContext);
        }
    }

    private static String appendImToolHint(String systemPrompt) {
        String hint = """
                你当前运行在 IM 会话中。系统已注入近期短历史；若需要更早或被引用消息的全文，请调用 im.read_history / im.read_nearby / im.read_message。\
                这些工具只能读取当前会话，禁止尝试其他群或他人私聊。""";
        if (!StringUtils.hasText(systemPrompt)) {
            return hint;
        }
        return systemPrompt.trim() + "\n\n" + hint;
    }

    private static List<Message> toSpringHistory(List<AgentCarrierHistoryMessageDTO> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>();
        for (AgentCarrierHistoryMessageDTO turn : history) {
            if (turn == null || !StringUtils.hasText(turn.getContent())) {
                continue;
            }
            if ("assistant".equalsIgnoreCase(turn.getRole())) {
                messages.add(new AssistantMessage(turn.getContent().trim()));
            } else {
                messages.add(new UserMessage(turn.getContent().trim()));
            }
        }
        return messages;
    }
}
