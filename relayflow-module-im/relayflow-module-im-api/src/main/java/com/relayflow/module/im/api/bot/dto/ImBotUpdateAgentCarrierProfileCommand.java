package com.relayflow.module.im.api.bot.dto;

import lombok.Data;

@Data
public class ImBotUpdateAgentCarrierProfileCommand {

    private Long tenantId;
    private Long agentId;
    private String name;
    private String description;
    private Long avatarFileId;
}
