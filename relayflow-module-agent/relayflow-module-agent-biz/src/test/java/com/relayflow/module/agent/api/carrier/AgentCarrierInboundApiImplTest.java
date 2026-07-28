package com.relayflow.module.agent.api.carrier;

import com.relayflow.module.agent.api.carrier.dto.AgentCarrierInboundCommand;
import com.relayflow.module.agent.builder.AgentBuilder;
import com.relayflow.module.agent.builder.RuntimeAgent;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import com.relayflow.module.agent.dal.mapper.AgentDefMapper;
import com.relayflow.module.agent.runtime.AgentRunCommand;
import com.relayflow.module.agent.runtime.AgentRunResult;
import com.relayflow.module.agent.runtime.AgentRuntime;
import com.relayflow.module.agent.skill.SkillRegistry;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentCarrierInboundApiImplTest {

    @Mock
    private AgentDefMapper agentDefMapper;
    @Mock
    private AgentBuilder agentBuilder;
    @Mock
    private AgentRuntime agentRuntime;
    @Mock
    private SkillRegistry skillRegistry;

    @InjectMocks
    private AgentCarrierInboundApiImpl api;

    @Test
    void handleInbound_runsRuntimeWithoutSessionSideEffects() {
        AgentDefDO agent = new AgentDefDO();
        agent.setId(9L);
        agent.setTenantId(1L);
        agent.setStatus("active");
        agent.setModelRef("demo");
        agent.setSkillIdsJson("[]");
        agent.setSystemPrompt("hi");
        agent.setName("A");

        when(agentDefMapper.selectOne(any(Wrapper.class))).thenReturn(agent);
        when(skillRegistry.parseSkillIdsJson("[]")).thenReturn(List.of());
        when(skillRegistry.resolveToolCallbacks(List.of())).thenReturn(List.of());
        when(agentBuilder.build(any(AgentDefDO.class), anyList())).thenReturn(RuntimeAgent.builder()
                .agentId(9L)
                .name("A")
                .systemPrompt("sys")
                .modelRef("demo")
                .tools(List.of())
                .build());
        when(agentRuntime.run(any(AgentRunCommand.class))).thenReturn(AgentRunResult.builder()
                .assistantText("ok")
                .build());

        AgentCarrierInboundCommand command = new AgentCarrierInboundCommand();
        command.setTenantId(1L);
        command.setAgentId(9L);
        command.setInboundText("ping");
        command.setConversationId(3L);

        assertEquals("ok", api.handleInbound(command));

        ArgumentCaptor<AgentRunCommand> captor = ArgumentCaptor.forClass(AgentRunCommand.class);
        verify(agentRuntime).run(captor.capture());
        assertEquals("ping", captor.getValue().getUserMessage());
        assertEquals(List.of(), captor.getValue().getHistory());
    }

    @Test
    void handleInbound_blankTextReturnsNull() {
        AgentCarrierInboundCommand command = new AgentCarrierInboundCommand();
        command.setTenantId(1L);
        command.setAgentId(9L);
        command.setInboundText("  ");
        assertNull(api.handleInbound(command));
    }
}
