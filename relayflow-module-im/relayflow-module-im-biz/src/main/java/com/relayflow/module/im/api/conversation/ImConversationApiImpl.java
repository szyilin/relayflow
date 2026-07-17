package com.relayflow.module.im.api.conversation;

import com.relayflow.module.im.api.conversation.dto.ConversationSearchRespDTO;
import com.relayflow.module.im.convert.ImConversationConvert;
import com.relayflow.module.im.service.conversation.ImConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImConversationApiImpl implements ImConversationApi {

    private static final int MAX_LIMIT = 10;

    private final ImConversationService conversationService;

    @Override
    public List<ConversationSearchRespDTO> searchConversations(Long tenantId, Long userId, String keyword, int limit) {
        if (tenantId == null || userId == null || !StringUtils.hasText(keyword)) {
            return List.of();
        }
        int safeLimit = clampLimit(limit);
        return conversationService.listConversations(tenantId, userId, keyword.trim()).stream()
                .limit(safeLimit)
                .map(ImConversationConvert.INSTANCE::toSearchDto)
                .toList();
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
