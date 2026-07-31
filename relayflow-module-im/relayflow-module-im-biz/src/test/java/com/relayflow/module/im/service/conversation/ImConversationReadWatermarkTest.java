package com.relayflow.module.im.service.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImConversationUserTagMapper;
import com.relayflow.module.im.dal.mapper.ImGroupMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.service.message.ImContentHelper;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.system.api.user.UserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImConversationReadWatermarkTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 11L;
    private static final long PEER_ID = 22L;
    private static final long CONVERSATION_ID = 1001L;

    @Mock
    private ImConversationMapper conversationMapper;
    @Mock
    private ImConversationMemberMapper conversationMemberMapper;
    @Mock
    private ImConversationUserTagMapper conversationUserTagMapper;
    @Mock
    private ImMessageMapper messageMapper;
    @Mock
    private ImGroupMapper groupMapper;
    @Mock
    private ImBotMapper botMapper;
    @Mock
    private UserApi userApi;
    @Mock
    private ImContentHelper contentHelper;
    @Mock
    private RealtimeTransportApi realtimeTransportApi;

    @InjectMocks
    private ImConversationServiceImpl conversationService;

    @Test
    void markConversationReadAdvancesBadgeAndFansOutReadUpdated() {
        ImConversationMemberDO member = new ImConversationMemberDO();
        member.setId(1L);
        member.setTenantId(TENANT_ID);
        member.setConversationId(CONVERSATION_ID);
        member.setSubjectType(ImMemberSubjectType.USER);
        member.setSubjectId(USER_ID);
        member.setReadSeq(3L);
        member.setBadgeSeq(3L);
        member.setUnreadCount(2);

        when(conversationMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(member);
        when(messageMapper.selectCount(any())).thenReturn(0L);
        when(conversationMemberMapper.selectList(any())).thenReturn(java.util.List.of(
                memberRow(PEER_ID, 0L)));

        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", java.util.List.of());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::requireLoginUser).thenReturn(loginUser);
            conversationService.markConversationRead(CONVERSATION_ID, 5L);
        }

        assertEquals(5L, member.getReadSeq());
        assertEquals(5L, member.getBadgeSeq());
        assertEquals(0, member.getUnreadCount());
        verify(conversationMemberMapper).updateById(member);

        ArgumentCaptor<RealtimeEnvelopeDTO> envelopeCaptor = ArgumentCaptor.forClass(RealtimeEnvelopeDTO.class);
        verify(realtimeTransportApi).sendToUsers(eq(TENANT_ID), eq(java.util.List.of(PEER_ID)), envelopeCaptor.capture());
        RealtimeEnvelopeDTO envelope = envelopeCaptor.getValue();
        assertEquals(ImRealtimeTypes.DOMAIN, envelope.getDomain());
        assertEquals(ImRealtimeTypes.READ_UPDATED, envelope.getType());
    }

    @Test
    void markInboxUnreadSetsBadgeToMaxMinusOneWithoutReadUpdated() {
        ImConversationMemberDO member = new ImConversationMemberDO();
        member.setId(1L);
        member.setTenantId(TENANT_ID);
        member.setConversationId(CONVERSATION_ID);
        member.setSubjectType(ImMemberSubjectType.USER);
        member.setSubjectId(USER_ID);
        member.setReadSeq(10L);
        member.setBadgeSeq(10L);
        member.setUnreadCount(0);

        ImMessageDO latest = new ImMessageDO();
        latest.setSeq(10L);

        when(conversationMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(member);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(latest);

        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", java.util.List.of());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::requireLoginUser).thenReturn(loginUser);
            conversationService.markInboxUnread(CONVERSATION_ID);
        }

        assertEquals(10L, member.getReadSeq());
        assertEquals(9L, member.getBadgeSeq());
        assertEquals(1, member.getUnreadCount());
        verify(conversationMemberMapper).updateById(member);
        verify(realtimeTransportApi, never()).sendToUsers(any(), any(), any());
    }

    private static ImConversationMemberDO memberRow(long userId, long readSeq) {
        ImConversationMemberDO row = new ImConversationMemberDO();
        row.setSubjectType(ImMemberSubjectType.USER);
        row.setSubjectId(userId);
        row.setReadSeq(readSeq);
        return row;
    }
}
