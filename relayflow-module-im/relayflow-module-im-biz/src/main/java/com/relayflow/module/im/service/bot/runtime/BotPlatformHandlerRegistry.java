package com.relayflow.module.im.service.bot.runtime;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BotPlatformHandlerRegistry {

    private final Map<String, BotPlatformHandler> handlersByCode;

    public BotPlatformHandlerRegistry(List<BotPlatformHandler> handlers) {
        Map<String, BotPlatformHandler> map = new HashMap<>();
        if (handlers != null) {
            for (BotPlatformHandler handler : handlers) {
                if (handler == null || handler.botCode() == null) {
                    continue;
                }
                map.put(handler.botCode(), handler);
            }
        }
        this.handlersByCode = Map.copyOf(map);
    }

    public BotPlatformHandler find(String botCode) {
        if (botCode == null) {
            return null;
        }
        return handlersByCode.get(botCode);
    }
}
