package com.relayflow.module.im.api.bot;

import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendResult;

/**
 * Cross-module business reach via Bot → {@code im_message} (bot_dm / group bot).
 * Sole write entry for business notifications; do not use system messages or Notify Inbox.
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
}
