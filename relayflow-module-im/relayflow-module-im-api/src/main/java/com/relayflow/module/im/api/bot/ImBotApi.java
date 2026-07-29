package com.relayflow.module.im.api.bot;

import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendResult;
import com.relayflow.module.im.api.bot.dto.ImBotTypingCommand;

/**
 * Cross-module business reach via Bot → {@code im_message} (bot_dm / group bot).
 * Sole write entry for business notifications; do not use system messages or Notify Inbox.
 * Also exposes Bot typing (ephemeral) so callers can show Telegram-style「正在输入」.
 */
public interface ImBotApi {

    /**
     * Send a bot message. Target scope {@code SINGLE} is required for V1 callers;
     * {@code ALL_ACTIVE_MEMBERSHIPS} fans out one delivery per ACTIVE membership.
     */
    ImBotSendResult send(ImBotSendCommand command);

    /**
     * When a user becomes ACTIVE in a tenant, auto-write user enablement for
     * tenant-enabled system bots with {@code mandatory} / {@code default_on} policy.
     * Does not create bot_dm conversations.
     */
    void ensureUserEnablementsOnActive(Long tenantId, Long userId);

    /**
     * Set/clear Bot typing for a conversation. Fans out {@code typing.updated} with
     * {@code senderType=bot}. Does not persist messages. Invalid bot/conversation → no-op.
     */
    void setTyping(ImBotTypingCommand command);
}
