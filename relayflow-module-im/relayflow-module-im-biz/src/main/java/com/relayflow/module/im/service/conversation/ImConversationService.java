package com.relayflow.module.im.service.conversation;

import com.relayflow.module.im.controller.app.vo.ConversationItemRespVO;

import java.util.List;

public interface ImConversationService {

    List<ConversationItemRespVO> listConversations(Long tenantId, Long userId, String keyword);

    Long getOrCreateDirectConversation(Long tenantId, Long userId, Long peerUserId);

    void requireMembership(Long tenantId, Long conversationId, Long userId);

    void lockConversation(Long tenantId, Long conversationId);

    List<Long> listOtherMemberUserIds(Long tenantId, Long conversationId, Long senderId);
}
