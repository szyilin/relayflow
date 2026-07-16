package com.relayflow.module.im.service.bot.runtime;

import lombok.Value;
import org.springframework.util.StringUtils;

/**
 * Optional reply produced by a platform handler. Empty text means no message.
 */
@Value
public class BotHandlerReply {

    String text;

    public static BotHandlerReply none() {
        return new BotHandlerReply(null);
    }

    public static BotHandlerReply text(String text) {
        return new BotHandlerReply(text);
    }

    public boolean hasText() {
        return StringUtils.hasText(text);
    }
}
