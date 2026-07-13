package com.relayflow.module.im.api.conversation;

import com.relayflow.module.im.api.conversation.dto.ConversationSearchRespDTO;

import java.util.List;

public interface ImConversationApi {

    List<ConversationSearchRespDTO> searchConversations(Long tenantId, Long userId, String keyword, int limit);
}
