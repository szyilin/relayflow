package com.relayflow.module.im.service.bot.runtime;

import com.relayflow.module.im.enums.ImBotHandlerKind;
import com.relayflow.module.im.service.message.ImMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BotRuntimeImplTest {

    private static final long TENANT_ID = 1L;
    private static final long CONVERSATION_ID = 301L;
    private static final long BOT_ID = 900003L;

    @Mock
    private ImMessageService messageService;

    private BotRuntimeImpl runtime;

    @BeforeEach
    void setUp() {
        // default empty registry; individual tests rebuild with handlers
        runtime = new BotRuntimeImpl(new BotPlatformHandlerRegistry(List.of()), messageService);
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
        runtime = new BotRuntimeImpl(new BotPlatformHandlerRegistry(List.of(echo)), messageService);

        runtime.dispatch(baseContext(ImBotHandlerKind.PLATFORM)
                .botCode("echo-bot")
                .botName("Echo")
                .inboundText("ping")
                .build());

        org.junit.jupiter.api.Assertions.assertTrue(called.get());
        verify(messageService).sendBotReply(TENANT_ID, CONVERSATION_ID, BOT_ID, "Echo", "pong");
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
        runtime = new BotRuntimeImpl(new BotPlatformHandlerRegistry(List.of(silent)), messageService);

        runtime.dispatch(baseContext(ImBotHandlerKind.PLATFORM).botCode("silent-bot").build());
        verify(messageService, never()).sendBotReply(anyLong(), anyLong(), anyLong(), any(), anyString());
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
        runtime = new BotRuntimeImpl(new BotPlatformHandlerRegistry(List.of(boom)), messageService);

        runtime.dispatch(baseContext(ImBotHandlerKind.PLATFORM).botCode("boom-bot").build());
        verify(messageService, never()).sendBotReply(any(), any(), any(), any(), any());
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
