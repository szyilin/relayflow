package com.relayflow.module.im.service.typing;

/**
 * Ephemeral conversation typing state (Redis TTL) + WS fanout.
 */
public interface ImTypingService {

    void startTyping(Long tenantId, Long userId, Long conversationId);

    void stopTyping(Long tenantId, Long userId, Long conversationId);

    /**
     * Bot typing (member subject BOT). Fans out to all user members of the conversation.
     */
    void startBotTyping(Long tenantId, Long botId, Long conversationId);

    void stopBotTyping(Long tenantId, Long botId, Long conversationId);
}
