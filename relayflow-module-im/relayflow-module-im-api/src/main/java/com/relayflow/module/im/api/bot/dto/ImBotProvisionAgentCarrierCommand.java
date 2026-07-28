package com.relayflow.module.im.api.bot.dto;

import lombok.Data;

@Data
public class ImBotProvisionAgentCarrierCommand {

    /** Tenant that owns the Agent (required). */
    private Long tenantId;

    /** AgentDef id (required). */
    private Long agentId;

    /** Display name for the bot (required). */
    private String name;

    private String description;

    private Long avatarFileId;

    /**
     * Agent owner user — receives tenant/user enablement and optional bot_dm seed.
     * Required for first-time provision UX.
     */
    private Long ownerUserId;
}
