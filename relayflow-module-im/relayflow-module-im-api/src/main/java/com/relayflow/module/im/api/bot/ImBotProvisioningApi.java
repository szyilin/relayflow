package com.relayflow.module.im.api.bot;

import com.relayflow.module.im.api.bot.dto.ImBotProvisionAgentCarrierCommand;
import com.relayflow.module.im.api.bot.dto.ImBotProvisionResult;
import com.relayflow.module.im.api.bot.dto.ImBotRetireAgentCarrierCommand;
import com.relayflow.module.im.api.bot.dto.ImBotUpdateAgentCarrierProfileCommand;

/**
 * Cross-module provisioning for Agent carrier bots ({@code type=tenant}, {@code handler_kind=platform}).
 * Agent domain MUST use this API instead of writing {@code im_*} tables.
 */
public interface ImBotProvisioningApi {

    /**
     * Idempotent create-or-ensure for {@code (tenantId, agentId)}.
     * Enables tenant (+ owner user) subscription and seeds bot_dm for the owner when {@code ownerUserId} is set.
     */
    ImBotProvisionResult provisionAgentCarrier(ImBotProvisionAgentCarrierCommand command);

    /** Disable carrier bot and soft-delete enablements. Idempotent. */
    void retireAgentCarrier(ImBotRetireAgentCarrierCommand command);

    /** Update display fields without changing code / owner_agent_id binding. */
    ImBotProvisionResult updateAgentCarrierProfile(ImBotUpdateAgentCarrierProfileCommand command);
}
