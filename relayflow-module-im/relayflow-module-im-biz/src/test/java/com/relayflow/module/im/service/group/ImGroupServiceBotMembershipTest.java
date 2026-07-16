package com.relayflow.module.im.service.group;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.im.controller.app.vo.GroupBotMembershipReqVO;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImGroupDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImBotTenantEnablementMapper;
import com.relayflow.module.im.dal.mapper.ImBotUserEnablementMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImGroupMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImBotType;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberRole;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.ImMessageService;
import com.relayflow.module.system.api.user.UserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImGroupServiceBotMembershipTest {

    private static final long TENANT_ID = 1L;
    private static final long OWNER_ID = 10L;
    private static final long MEMBER_ID = 20L;
    private static final long CONVERSATION_ID = 301L;
    private static final long BOT_ID = 900003L;

    @Mock
    private ImConversationMapper conversationMapper;
    @Mock
    private ImConversationMemberMapper conversationMemberMapper;
    @Mock
    private ImGroupMapper groupMapper;
    @Mock
    private ImBotMapper botMapper;
    @Mock
    private ImBotTenantEnablementMapper tenantEnablementMapper;
    @Mock
    private ImBotUserEnablementMapper userEnablementMapper;
    @Mock
    private ImConversationService conversationService;
    @Mock
    private ImMessageService messageService;
    @Mock
    private UserApi userApi;

    @InjectMocks
    private ImGroupServiceImpl groupService;

    private MockedStatic<com.relayflow.framework.security.core.SecurityFrameworkUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(com.relayflow.framework.security.core.SecurityFrameworkUtils.class);
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    @Test
    void addBot_rejectsNonOwner() {
        loginAs(MEMBER_ID);
        stubGroup();
        stubMembershipRole(MEMBER_ID, ImMemberRole.MEMBER);

        GroupBotMembershipReqVO request = new GroupBotMembershipReqVO();
        request.setConversationId(CONVERSATION_ID);
        request.setBotCode("task-bot");

        ServiceException ex = assertThrows(ServiceException.class, () -> groupService.addBot(request));
        assertEquals(ErrorCodeConstants.GROUP_OWNER_REQUIRED.getCode(), ex.getCode());
        verify(botMapper, never()).selectOne(any());
    }

    @Test
    void addBot_idempotentWhenAlreadyMember() {
        loginAs(OWNER_ID);
        stubGroup();
        stubMembershipRole(OWNER_ID, ImMemberRole.OWNER);
        when(botMapper.selectOne(any())).thenReturn(systemBot());
        when(conversationMemberMapper.selectOne(any())).thenAnswer(invocation -> {
            // first call: owner membership already stubbed above via thenReturn sequence —
            // re-stub carefully
            return null;
        });

        // owner check uses selectOne; bot membership also uses selectOne — use consecutive returns
        ImConversationMemberDO owner = userMembership(OWNER_ID, ImMemberRole.OWNER);
        ImConversationMemberDO existingBot = botMembership();
        when(conversationMemberMapper.selectOne(any()))
                .thenReturn(owner)
                .thenReturn(existingBot);

        GroupBotMembershipReqVO request = new GroupBotMembershipReqVO();
        request.setConversationId(CONVERSATION_ID);
        request.setBotCode("task-bot");

        assertTrue(!groupService.addBot(request).isAdded());
        verify(conversationMemberMapper, never()).insert(any(ImConversationMemberDO.class));
        verify(messageService, never()).sendSystemMessage(any(), any(), any());
    }

    @Test
    void addBot_insertsAndSendsSystemTip() {
        loginAs(OWNER_ID);
        stubGroup();
        ImConversationMemberDO owner = userMembership(OWNER_ID, ImMemberRole.OWNER);
        when(conversationMemberMapper.selectOne(any()))
                .thenReturn(owner)
                .thenReturn(null);
        when(botMapper.selectOne(any())).thenReturn(systemBot());

        GroupBotMembershipReqVO request = new GroupBotMembershipReqVO();
        request.setConversationId(CONVERSATION_ID);
        request.setBotCode("task-bot");

        assertTrue(groupService.addBot(request).isAdded());
        verify(conversationMemberMapper).insert(any(ImConversationMemberDO.class));
        verify(messageService).sendSystemMessage(TENANT_ID, CONVERSATION_ID, "任务助手 加入了群聊");
    }

    private void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "u", TENANT_ID, "member", List.of());
        security.when(com.relayflow.framework.security.core.SecurityFrameworkUtils::requireLoginUser)
                .thenReturn(loginUser);
    }

    private void stubGroup() {
        ImGroupDO group = new ImGroupDO();
        group.setId(401L);
        group.setTenantId(TENANT_ID);
        group.setConversationId(CONVERSATION_ID);
        when(groupMapper.selectOne(any())).thenReturn(group);

        com.relayflow.module.im.dal.dataobject.ImConversationDO conversation =
                new com.relayflow.module.im.dal.dataobject.ImConversationDO();
        conversation.setId(CONVERSATION_ID);
        conversation.setTenantId(TENANT_ID);
        conversation.setType(ImConversationType.GROUP);
        when(conversationService.requireConversation(TENANT_ID, CONVERSATION_ID)).thenReturn(conversation);
    }

    private void stubMembershipRole(Long userId, String role) {
        when(conversationMemberMapper.selectOne(any())).thenReturn(userMembership(userId, role));
    }

    private ImConversationMemberDO userMembership(Long userId, String role) {
        ImConversationMemberDO membership = new ImConversationMemberDO();
        membership.setId(1L);
        membership.setTenantId(TENANT_ID);
        membership.setConversationId(CONVERSATION_ID);
        membership.setSubjectType(ImMemberSubjectType.USER);
        membership.setSubjectId(userId);
        membership.setRole(role);
        return membership;
    }

    private ImConversationMemberDO botMembership() {
        ImConversationMemberDO membership = new ImConversationMemberDO();
        membership.setId(2L);
        membership.setTenantId(TENANT_ID);
        membership.setConversationId(CONVERSATION_ID);
        membership.setSubjectType(ImMemberSubjectType.BOT);
        membership.setSubjectId(BOT_ID);
        membership.setRole(ImMemberRole.MEMBER);
        return membership;
    }

    private ImBotDO systemBot() {
        ImBotDO bot = new ImBotDO();
        bot.setId(BOT_ID);
        bot.setCode("task-bot");
        bot.setName("任务助手");
        bot.setType(ImBotType.SYSTEM);
        bot.setStatus(1);
        return bot;
    }
}
