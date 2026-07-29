package com.relayflow.module.agent.api.carrier;

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
import com.relayflow.module.agent.skill.im.ImReadHistorySkill;
import com.relayflow.module.agent.skill.im.ImReadMessageSkill;
import com.relayflow.module.agent.skill.im.ImReadNearbySkill;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private CapabilityAssemblyService capabilityAssemblyService;

    @InjectMocks
    private AgentCarrierInboundApiImpl api;

    @Test
    void handleInbound_runsRuntimeWithHistoryAndImCarrierPack() {
        AgentDefDO agent = new AgentDefDO();
        agent.setId(9L);
        agent.setTenantId(1L);
        agent.setStatus("active");
        agent.setModelRef("demo");
        agent.setSkillIdsJson("[]");
        agent.setSystemPrompt("hi");
        agent.setName("A");

        when(agentDefMapper.selectOne(any(Wrapper.class))).thenReturn(agent);
        when(capabilityAssemblyService.assemble(eq(agent), eq(AgentChannel.IM_BOT)))
                .thenReturn(CapabilityAssembly.builder()
                        .hotSkillCodes(List.of(
                                ImReadMessageSkill.CODE,
                                ImReadNearbySkill.CODE,
                                ImReadHistorySkill.CODE))
                        .deferredDomains(List.of())
                        .authorizedDomains(List.of("im"))
                        .domainSummaryAppendix("")
                        .metaToolsAttached(false)
                        .tools(List.of())
                        .hotSkillCodeSet(Set.of(
                                ImReadMessageSkill.CODE,
                                ImReadNearbySkill.CODE,
                                ImReadHistorySkill.CODE))
                        .deferredSkillCodeSet(Set.of())
                        .build());
        when(agentBuilder.build(any(AgentDefDO.class), anyList(), any())).thenReturn(RuntimeAgent.builder()
                .agentId(9L)
                .name("A")
                .systemPrompt("sys")
                .modelRef("demo")
                .tools(List.of())
                .build());
        when(agentRuntime.run(any(AgentRunCommand.class))).thenReturn(AgentRunResult.builder()
                .assistantText("ok")
                .build());

        AgentCarrierHistoryMessageDTO prior = new AgentCarrierHistoryMessageDTO();
        prior.setRole("user");
        prior.setContent("earlier");
        AgentCarrierHistoryMessageDTO priorBot = new AgentCarrierHistoryMessageDTO();
        priorBot.setRole("assistant");
        priorBot.setContent("reply");

        AgentCarrierInboundCommand command = new AgentCarrierInboundCommand();
        command.setTenantId(1L);
        command.setAgentId(9L);
        command.setInboundText("ping");
        command.setConversationId(3L);
        command.setTriggerUserId(42L);
        command.setBotId(7L);
        command.setHistory(List.of(prior, priorBot));

        assertEquals("ok", api.handleInbound(command));

        ArgumentCaptor<CapabilityAssembly> unused = ArgumentCaptor.forClass(CapabilityAssembly.class);
        verify(capabilityAssemblyService).assemble(eq(agent), eq(AgentChannel.IM_BOT));

        ArgumentCaptor<AgentRunCommand> captor = ArgumentCaptor.forClass(AgentRunCommand.class);
        verify(agentRuntime).run(captor.capture());
        assertEquals("ping", captor.getValue().getUserMessage());
        assertEquals(2, captor.getValue().getHistory().size());
        assertTrue(captor.getValue().getHistory().get(0) instanceof UserMessage);
        assertTrue(captor.getValue().getHistory().get(1) instanceof AssistantMessage);
        assertTrue(captor.getValue().getSystemPrompt().contains("im.read_history"));
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
