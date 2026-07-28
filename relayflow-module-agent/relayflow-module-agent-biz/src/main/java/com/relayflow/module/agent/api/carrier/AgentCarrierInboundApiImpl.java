package com.relayflow.module.agent.api.carrier;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.agent.api.carrier.dto.AgentCarrierInboundCommand;
import com.relayflow.module.agent.builder.AgentBuilder;
import com.relayflow.module.agent.builder.RuntimeAgent;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import com.relayflow.module.agent.dal.mapper.AgentDefMapper;
import com.relayflow.module.agent.runtime.AgentRunCommand;
import com.relayflow.module.agent.runtime.AgentRunResult;
import com.relayflow.module.agent.runtime.AgentRuntime;
import com.relayflow.module.agent.skill.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

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
    private final SkillRegistry skillRegistry;

    @Override
    public String handleInbound(AgentCarrierInboundCommand command) {
        if (command == null || command.getTenantId() == null || command.getAgentId() == null
                || !StringUtils.hasText(command.getInboundText())) {
            return null;
        }
        try {
            AgentDefDO agent = agentDefMapper.selectOne(Wrappers.<AgentDefDO>lambdaQuery()
                    .eq(AgentDefDO::getTenantId, command.getTenantId())
                    .eq(AgentDefDO::getId, command.getAgentId()));
            if (agent == null || !STATUS_ACTIVE.equals(agent.getStatus())) {
                return null;
            }
            List<String> skillIds = skillRegistry.parseSkillIdsJson(agent.getSkillIdsJson());
            List<ToolCallback> tools = skillRegistry.resolveToolCallbacks(skillIds);
            RuntimeAgent runtimeAgent = agentBuilder.build(agent, tools);
            AgentRunResult result = agentRuntime.run(AgentRunCommand.builder()
                    .modelRef(runtimeAgent.getModelRef())
                    .systemPrompt(runtimeAgent.getSystemPrompt())
                    .history(List.of())
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
        }
    }
}
