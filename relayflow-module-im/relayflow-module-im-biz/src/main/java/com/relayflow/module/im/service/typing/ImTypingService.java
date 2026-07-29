package com.relayflow.module.im.service.typing;

/**
 * Ephemeral conversation typing state (Redis TTL) + WS fanout.
 */
public interface ImTypingService {

    void startTyping(Long tenantId, Long userId, Long conversationId);

    void stopTyping(Long tenantId, Long userId, Long conversationId);
}
