package com.relayflow.module.im.service.bot.ingress;

import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.service.bot.runtime.BotInboundContext;
import com.relayflow.module.im.service.message.ImContentHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupBotMentionDispatcherTest {

    private static final long TENANT_ID = 1L;
    private static final long CONVERSATION_ID = 301L;
    private static final long USER_ID = 10L;
    private static final long MESSAGE_ID = 5001L;
    private static final long BOT_ID = 900003L;

    @Mock
    private ImConversationMapper conversationMapper;
    @Mock
    private ImConversationMemberMapper conversationMemberMapper;
    @Mock
    private ImBotMapper botMapper;
    @Mock
    private BotIngress botIngress;

    private GroupBotMentionDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        ImContentHelper contentHelper = new ImContentHelper(new ObjectMapper(), null);
        dispatcher = new GroupBotMentionDispatcher(
                contentHelper, conversationMapper, conversationMemberMapper, botMapper, botIngress);
    }

    @Test
    void memberBotMentionInvokesIngress() {
        stubGroup();
        stubBotMember(BOT_ID);
        when(botMapper.selectOne(any())).thenReturn(systemBot());

        dispatcher.dispatchAfterUserMessage(TENANT_ID, CONVERSATION_ID, USER_ID, MESSAGE_ID, contentWithMention());

        ArgumentCaptor<BotInboundContext> captor = ArgumentCaptor.forClass(BotInboundContext.class);
        verify(botIngress).onInbound(captor.capture());
        assertEquals(BOT_ID, captor.getValue().getBotId());
        assertEquals("task-bot", captor.getValue().getBotCode());
        assertEquals(MESSAGE_ID, captor.getValue().getTriggerMessageId());
    }

    @Test
    void nonMemberBotMentionIsIgnored() {
        stubGroup();
        when(conversationMemberMapper.selectList(any())).thenReturn(List.of());

        dispatcher.dispatchAfterUserMessage(TENANT_ID, CONVERSATION_ID, USER_ID, MESSAGE_ID, contentWithMention());

        verify(botIngress, never()).onInbound(any());
        verify(botMapper, never()).selectOne(any());
    }

    @Test
    void ingressFailureDoesNotPropagate() {
        stubGroup();
        stubBotMember(BOT_ID);
        when(botMapper.selectOne(any())).thenReturn(systemBot());
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(botIngress).onInbound(any());

        dispatcher.dispatchAfterUserMessage(TENANT_ID, CONVERSATION_ID, USER_ID, MESSAGE_ID, contentWithMention());

        verify(botIngress).onInbound(any());
    }

    private void stubGroup() {
        ImConversationDO conversation = new ImConversationDO();
        conversation.setId(CONVERSATION_ID);
        conversation.setTenantId(TENANT_ID);
        conversation.setType(ImConversationType.GROUP);
        when(conversationMapper.selectById(CONVERSATION_ID)).thenReturn(conversation);
    }

    private void stubBotMember(Long botId) {
        ImConversationMemberDO member = new ImConversationMemberDO();
        member.setSubjectType(ImMemberSubjectType.BOT);
        member.setSubjectId(botId);
        when(conversationMemberMapper.selectList(any())).thenReturn(List.of(member));
    }

    private ImBotDO systemBot() {
        ImBotDO bot = new ImBotDO();
        bot.setId(BOT_ID);
        bot.setCode("task-bot");
        bot.setName("任务助手");
        bot.setHandlerKind("noop");
        bot.setStatus(1);
        return bot;
    }

    private MessageContentVO contentWithMention() {
        ContentBlockVO mention = new ContentBlockVO();
        mention.setType("mention");
        mention.setSubjectType("bot");
        mention.setBotCode("task-bot");
        mention.setText("@任务助手");
        ContentBlockVO text = new ContentBlockVO();
        text.setType("text");
        text.setText(" 帮我看看");
        MessageContentVO content = new MessageContentVO();
        content.setVersion(1);
        content.setBlocks(List.of(mention, text));
        return content;
    }
}
