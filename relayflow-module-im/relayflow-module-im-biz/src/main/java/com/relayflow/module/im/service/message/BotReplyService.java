package com.relayflow.module.im.service.message;

/**
 * Narrow port for bot text replies — breaks BotRuntime ↔ ImMessageService construct cycle.
 */
public interface BotReplyService {

    void sendBotReply(Long tenantId, Long conversationId, Long botId, String botName, String text);
}
