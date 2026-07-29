package com.relayflow.module.im.api.conversation;

import com.relayflow.module.im.api.conversation.dto.ImMessageSummaryDTO;
import com.relayflow.module.im.api.conversation.dto.ImReadPrincipal;

import java.util.List;

/**
 * Cross-domain programmatic read of IM messages. ACL is enforced in im-biz.
 * Callers MUST construct {@link ImReadPrincipal} server-side; never trust model IDs alone.
 */
public interface ImConversationReadApi {

    /**
     * @return message summary, or {@code null} if not found / not visible (no leak)
     */
    ImMessageSummaryDTO getMessage(ImReadPrincipal principal, Long messageId);

    /**
     * Messages around {@code aroundSeq} in the allowed conversation (chronological).
     * Excludes the message at {@code aroundSeq} itself when present.
     */
    List<ImMessageSummaryDTO> listNearby(ImReadPrincipal principal, Long conversationId,
                                         long aroundSeq, int before, int after);

    /**
     * Messages strictly before {@code beforeSeq} (chronological), newest-first window then reversed.
     */
    List<ImMessageSummaryDTO> listHistory(ImReadPrincipal principal, Long conversationId,
                                          long beforeSeq, int limit);
}
