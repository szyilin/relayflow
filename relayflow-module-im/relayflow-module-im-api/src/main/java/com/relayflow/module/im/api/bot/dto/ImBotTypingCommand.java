package com.relayflow.module.im.api.bot.dto;

import lombok.Data;

/**
 * Bot ephemeral typing indicator (IM capability; callers adapt to this).
 */
@Data
public class ImBotTypingCommand {

    /** Bot code, e.g. agent-carrier-{id} */
    private String botCode;

    private Long conversationId;

    /** true = start/renew, false = stop */
    private boolean typing;
}
