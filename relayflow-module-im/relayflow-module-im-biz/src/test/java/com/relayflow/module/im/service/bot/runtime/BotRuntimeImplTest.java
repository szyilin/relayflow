package com.relayflow.module.im.service.bot.runtime;

import com.relayflow.module.agent.api.carrier.AgentCarrierInboundApi;
import com.relayflow.module.agent.api.carrier.dto.AgentCarrierHistoryMessageDTO;
import com.relayflow.module.agent.api.carrier.dto.AgentCarrierInboundCommand;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.enums.ImBotGroupReplyMentionMode;
import com.relayflow.module.im.enums.ImBotHandlerKind;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.service.bot.ImBotService;
import com.relayflow.module.im.service.bot.ingress.CarrierInboundContextAssembler;
import com.relayflow.module.im.service.message.ImMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotRuntimeImplTest {

    private static final long TENANT_ID = 1L;
    private static final long CONVERSATION_ID = 301L;
    private static final long BOT_ID = 900003L;
    private static final long AGENT_ID = 42L;
    private static final long TRIGGER_USER_ID = 77L;

    @Mock
    private ImMessageService messageService;

    @Mock
    private ImBotService imBotService;

    @Mock
    private CarrierInboundContextAssembler carrierInboundContextAssembler;

    @Mock
    private ObjectProvider<AgentCarrierInboundApi> agentCarrierInboundApi;

    @Mock
    private AgentCarrierInboundApi carrierInboundApi;

    @Mock
    private ImBotMapper botMapper;

    @Mock
    private ImConversationMapper conversationMapper;

    private BotRuntimeImpl runtime;

    @BeforeEach
    void setUp() {
        lenient().when(agentCarrierInboundApi.getIfAvailable()).thenReturn(null);
        lenient().when(carrierInboundContextAssembler.assemble(any()))
                .thenAnswer(inv -> {
                    BotInboundContext ctx = inv.getArgument(0);
                    String text = ctx == null ? null : ctx.getInboundText();
                    return new CarrierInboundContextAssembler.Assembled(text, List.<AgentCarrierHistoryMessageDTO>of());
                });
        runtime = newRuntime(List.of());
    }

    private BotRuntimeImpl newRuntime(List<BotPlatformHandler> handlers) {
        return new BotRuntimeImpl(
                new BotPlatformHandlerRegistry(handlers),
                messageService,
                imBotService,
                carrierInboundContextAssembler,
                agentCarrierInboundApi,
                botMapper,
                conversationMapper);
    }

    @Test
    void noopProducesNoReply() {
        runtime.dispatch(baseContext(ImBotHandlerKind.NOOP).build());
        verifyNoInteractions(messageService);
    }

    @Test
    void webhookStubDoesNotCallMessageService() {
        runtime.dispatch(baseContext(ImBotHandlerKind.WEBHOOK).botCode("ext-bot").build());
        verifyNoInteractions(messageService);
    }

    @Test
    void platformMissingHandlerIsNoop() {
        runtime.dispatch(baseContext(ImBotHandlerKind.PLATFORM).botCode("task-bot").build());
        verifyNoInteractions(messageService);
    }

    @Test
    void platformHandlerReplyPersistsBotMessage() {
        AtomicBoolean called = new AtomicBoolean(false);
        BotPlatformHandler echo = new BotPlatformHandler() {
            @Override
            public String botCode() {
                return "echo-bot";
            }

            @Override
            public BotHandlerReply handle(BotInboundContext context) {
                called.set(true);
                return BotHandlerReply.text("pong");
            }
        };
        runtime = newRuntime(List.of(echo));
        stubGroupTriggerMention();

        runtime.dispatch(baseContext(ImBotHandlerKind.PLATFORM)
                .botCode("echo-bot")
                .botName("Echo")
                .inboundText("ping")
                .triggerUserId(TRIGGER_USER_ID)
                .build());

        assertTrue(called.get());
        verify(messageService).sendBotReply(TENANT_ID, CONVERSATION_ID, BOT_ID, "Echo", "pong", TRIGGER_USER_ID);
    }

    @Test
    void platformHandlerEmptyReplySkipsPersist() {
        BotPlatformHandler silent = new BotPlatformHandler() {
            @Override
            public String botCode() {
                return "silent-bot";
            }

            @Override
            public BotHandlerReply handle(BotInboundContext context) {
                return BotHandlerReply.none();
            }
        };
        runtime = newRuntime(List.of(silent));

        runtime.dispatch(baseContext(ImBotHandlerKind.PLATFORM).botCode("silent-bot").build());
        verify(messageService, never()).sendBotReply(anyLong(), anyLong(), anyLong(), any(), anyString(), any());
    }

    @Test
    void handlerExceptionDoesNotPropagate() {
        BotPlatformHandler boom = new BotPlatformHandler() {
            @Override
            public String botCode() {
                return "boom-bot";
            }

            @Override
            public BotHandlerReply handle(BotInboundContext context) {
                throw new RuntimeException("boom");
            }
        };
        runtime = newRuntime(List.of(boom));

        runtime.dispatch(baseContext(ImBotHandlerKind.PLATFORM).botCode("boom-bot").build());
        verify(messageService, never()).sendBotReply(any(), any(), any(), any(), any(), any());
    }

    @Test
    void carrierOwnerAgentRoutesToAgentApi() {
        when(agentCarrierInboundApi.getIfAvailable()).thenReturn(carrierInboundApi);
        when(carrierInboundApi.handleInbound(any(AgentCarrierInboundCommand.class))).thenReturn("agent says hi");
        runtime = newRuntime(List.of());
        stubGroupTriggerMention();

        runtime.dispatch(baseContext(ImBotHandlerKind.PLATFORM)
                .ownerAgentId(AGENT_ID)
                .botName("助手")
                .inboundText("hello")
                .triggerUserId(TRIGGER_USER_ID)
                .build());

        verify(carrierInboundApi).handleInbound(any(AgentCarrierInboundCommand.class));
        verify(messageService).sendBotReply(
                eq(TENANT_ID), eq(CONVERSATION_ID), eq(BOT_ID), eq("助手"), eq("agent says hi"), eq(TRIGGER_USER_ID));
    }

    @Test
    void groupReplyMentionNoneSkipsMention() {
        runtime = newRuntime(List.of());
        ImConversationDO group = new ImConversationDO();
        group.setType(ImConversationType.GROUP);
        when(conversationMapper.selectById(CONVERSATION_ID)).thenReturn(group);
        ImBotDO bot = new ImBotDO();
        bot.setGroupReplyMentionMode(ImBotGroupReplyMentionMode.NONE);
        when(botMapper.selectById(BOT_ID)).thenReturn(bot);

        Long mentionId = runtime.resolveGroupReplyMentionUserId(baseContext(ImBotHandlerKind.PLATFORM)
                .triggerUserId(TRIGGER_USER_ID)
                .build());
        assertNull(mentionId);
    }

    @Test
    void botDmDoesNotAutoMention() {
        runtime = newRuntime(List.of());
        ImConversationDO botDm = new ImConversationDO();
        botDm.setType(ImConversationType.BOT_DM);
        when(conversationMapper.selectById(CONVERSATION_ID)).thenReturn(botDm);

        Long mentionId = runtime.resolveGroupReplyMentionUserId(baseContext(ImBotHandlerKind.PLATFORM)
                .triggerUserId(TRIGGER_USER_ID)
                .build());
        assertNull(mentionId);
    }

    @Test
    void groupTriggerDefaultsToMention() {
        runtime = newRuntime(List.of());
        stubGroupTriggerMention();
        Long mentionId = runtime.resolveGroupReplyMentionUserId(baseContext(ImBotHandlerKind.PLATFORM)
                .triggerUserId(TRIGGER_USER_ID)
                .build());
        assertEquals(TRIGGER_USER_ID, mentionId);
    }

    private void stubGroupTriggerMention() {
        ImConversationDO group = new ImConversationDO();
        group.setType(ImConversationType.GROUP);
        lenient().when(conversationMapper.selectById(CONVERSATION_ID)).thenReturn(group);
        ImBotDO bot = new ImBotDO();
        bot.setGroupReplyMentionMode(ImBotGroupReplyMentionMode.TRIGGER);
        lenient().when(botMapper.selectById(BOT_ID)).thenReturn(bot);
    }

    private BotInboundContext.BotInboundContextBuilder baseContext(String handlerKind) {
        return BotInboundContext.builder()
                .tenantId(TENANT_ID)
                .conversationId(CONVERSATION_ID)
                .botId(BOT_ID)
                .botCode("task-bot")
                .handlerKind(handlerKind)
                .botName("任务助手");
    }
}
