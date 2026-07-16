package com.relayflow.module.im.service.card;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.api.bot.dto.card.ImBotCardDocument;
import com.relayflow.module.im.api.card.CardActionContext;
import com.relayflow.module.im.api.card.CardActionHandler;
import com.relayflow.module.im.api.card.CardActionResult;
import com.relayflow.module.im.controller.app.vo.CardActionReqVO;
import com.relayflow.module.im.controller.app.vo.CardActionRespVO;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.controller.app.vo.card.CardActionItemVO;
import com.relayflow.module.im.controller.app.vo.card.CardBehaviorVO;
import com.relayflow.module.im.controller.app.vo.card.CardHeaderVO;
import com.relayflow.module.im.controller.app.vo.card.CardMetaVO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImSenderType;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.ImContentHelper;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardActionIngressImplTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 10L;
    private static final long CONVERSATION_ID = 501L;
    private static final long MESSAGE_ID = 1001L;

    @Mock
    private ImConversationService conversationService;
    @Mock
    private ImMessageMapper messageMapper;
    @Mock
    private ImConversationMapper conversationMapper;
    @Mock
    private ImBotMapper botMapper;
    @Mock
    private ImContentHelper contentHelper;
    @Mock
    private RealtimeTransportApi realtimeTransportApi;

    private AtomicInteger handlerCalls;
    private CardActionIngressImpl ingress;

    @BeforeEach
    void setUp() {
        handlerCalls = new AtomicInteger();
        CardActionHandler demo = new CardActionHandler() {
            @Override
            public String actionKey() {
                return "demo.card.ack";
            }

            @Override
            public CardActionResult handle(CardActionContext context) {
                handlerCalls.incrementAndGet();
                return CardActionResult.toastOnly("success", "ok");
            }
        };
        ingress = new CardActionIngressImpl(
                conversationService,
                messageMapper,
                conversationMapper,
                botMapper,
                contentHelper,
                new CardActionHandlerRegistry(List.of(demo)),
                realtimeTransportApi);
    }

    @Test
    void expiredCardRejectsWithoutHandler() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::requireLoginUser).thenReturn(loginUser());
            doNothing().when(conversationService).requireMembership(TENANT_ID, CONVERSATION_ID, USER_ID);

            ImMessageDO message = baseMessage();
            when(messageMapper.selectOne(any())).thenReturn(message);

            MessageContentVO content = cardContent("2000-01-01T00:00:00Z");
            when(contentHelper.fromJson(any())).thenReturn(content);
            when(contentHelper.firstCardBlock(content)).thenReturn(content.getBlocks().get(0));

            CardActionReqVO req = baseRequest();
            ServiceException ex = assertThrows(ServiceException.class, () -> ingress.handle(req));
            assertEquals(ErrorCodeConstants.CARD_ACTION_EXPIRED.getCode(), ex.getCode());
            assertEquals(0, handlerCalls.get());
        }
    }

    @Test
    void clientActionIdIsIdempotent() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::requireLoginUser).thenReturn(loginUser());
            doNothing().when(conversationService).requireMembership(TENANT_ID, CONVERSATION_ID, USER_ID);

            ImMessageDO message = baseMessage();
            when(messageMapper.selectOne(any())).thenReturn(message);

            MessageContentVO content = cardContent("2030-01-01T00:00:00Z");
            when(contentHelper.fromJson(any())).thenReturn(content);
            when(contentHelper.firstCardBlock(content)).thenReturn(content.getBlocks().get(0));
            when(botMapper.selectById(anyLong())).thenReturn(null);

            CardActionReqVO req = baseRequest();
            CardActionRespVO first = ingress.handle(req);
            CardActionRespVO second = ingress.handle(req);

            assertNotNull(first.getToast());
            assertEquals("ok", first.getToast().getContent());
            assertEquals(1, handlerCalls.get());
            assertEquals(first.getToast().getContent(), second.getToast().getContent());
        }
    }

    @Test
    void cardDocumentValidationRequiresGenericV1() {
        ImBotCardDocument card = new ImBotCardDocument();
        card.setTemplate("other.v1");
        assertThrows(ServiceException.class, () -> ImBotCardSupport.validate(card));
    }

    @Test
    void cardDocumentValidationAcceptsGenericV1() {
        ImBotCardDocument card = new ImBotCardDocument();
        card.setTemplate(ImBotCardSupport.TEMPLATE_GENERIC_V1);
        ImBotCardDocument.ImBotCardHeader header = new ImBotCardDocument.ImBotCardHeader();
        header.setTitle("标题");
        card.setHeader(header);
        ImBotCardDocument.ImBotCardAction action = new ImBotCardDocument.ImBotCardAction();
        action.setId("a1");
        action.setLabel("打开");
        ImBotCardDocument.ImBotCardBehavior behavior = new ImBotCardDocument.ImBotCardBehavior();
        behavior.setType(ImBotCardSupport.BEHAVIOR_OPEN_URL);
        behavior.setRoute("/app/messages");
        action.setBehavior(behavior);
        card.setActions(List.of(action));

        ContentBlockVO block = ImBotCardSupport.toContentBlock(card);
        assertEquals("card", block.getType());
        assertEquals("generic.v1", block.getTemplate());
        assertTrue(block.getCardId().startsWith("c_"));
    }

    private LoginUser loginUser() {
        return new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
    }

    private ImMessageDO baseMessage() {
        ImMessageDO message = new ImMessageDO();
        message.setId(MESSAGE_ID);
        message.setTenantId(TENANT_ID);
        message.setConversationId(CONVERSATION_ID);
        message.setSenderId(900001L);
        message.setSenderType(ImSenderType.BOT);
        message.setType(ImContentHelper.MESSAGE_TYPE_CARD);
        message.setContentJson("{}");
        message.setSeq(1L);
        message.setCreateTime(OffsetDateTime.now());
        return message;
    }

    private MessageContentVO cardContent(String expiresAt) {
        ContentBlockVO card = new ContentBlockVO();
        card.setType(ImContentHelper.BLOCK_TYPE_CARD);
        card.setCardId("c1");
        card.setTemplate(ImBotCardSupport.TEMPLATE_GENERIC_V1);
        CardHeaderVO header = new CardHeaderVO();
        header.setTitle("演示");
        card.setHeader(header);
        CardMetaVO meta = new CardMetaVO();
        meta.setExpiresAt(expiresAt);
        card.setMeta(meta);

        CardActionItemVO action = new CardActionItemVO();
        action.setId("ack");
        action.setLabel("确认");
        CardBehaviorVO behavior = new CardBehaviorVO();
        behavior.setType(ImBotCardSupport.BEHAVIOR_CALLBACK);
        behavior.setActionKey("demo.card.ack");
        behavior.setPayload(Map.of("demo", true));
        action.setBehavior(behavior);
        card.setActions(List.of(action));

        MessageContentVO content = new MessageContentVO();
        content.setVersion(1);
        content.setBlocks(List.of(card));
        return content;
    }

    private CardActionReqVO baseRequest() {
        CardActionReqVO req = new CardActionReqVO();
        req.setMessageId(MESSAGE_ID);
        req.setConversationId(CONVERSATION_ID);
        req.setActionId("ack");
        req.setActionKey("demo.card.ack");
        req.setClientActionId("client-1");
        return req;
    }
}
