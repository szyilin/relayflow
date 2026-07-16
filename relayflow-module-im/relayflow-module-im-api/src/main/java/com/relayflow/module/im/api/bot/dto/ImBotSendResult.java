package com.relayflow.module.im.api.bot.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImBotSendResult {

    /** Message ids created or returned by dedupe (one per delivery tenant). */
    private List<Long> messageIds = new ArrayList<>();

    /** True when every delivery hit an existing dedupe key (no new unread message). */
    private boolean dedupeHit;
}
