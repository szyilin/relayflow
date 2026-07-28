package com.relayflow.module.im.api.bot.dto;

import lombok.Data;

@Data
public class ImBotRetireAgentCarrierCommand {

    private Long tenantId;

    /** Preferred lookup key. */
    private Long agentId;

    /** Optional; used when agentId lookup is not enough. */
    private Long botId;
}
