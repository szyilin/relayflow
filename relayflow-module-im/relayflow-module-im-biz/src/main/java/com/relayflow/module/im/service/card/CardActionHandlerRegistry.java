package com.relayflow.module.im.service.card;

import com.relayflow.module.im.api.card.CardActionHandler;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CardActionHandlerRegistry {

    private final Map<String, CardActionHandler> handlersByKey;

    public CardActionHandlerRegistry(List<CardActionHandler> handlers) {
        Map<String, CardActionHandler> map = new HashMap<>();
        if (handlers != null) {
            for (CardActionHandler handler : handlers) {
                if (handler == null || !StringUtils.hasText(handler.actionKey())) {
                    continue;
                }
                String key = handler.actionKey().trim();
                if (map.put(key, handler) != null) {
                    throw new IllegalStateException("Duplicate CardActionHandler for actionKey=" + key);
                }
            }
        }
        this.handlersByKey = Map.copyOf(map);
    }

    public CardActionHandler require(String actionKey) {
        if (!StringUtils.hasText(actionKey)) {
            throw new ServiceException(ErrorCodeConstants.CARD_ACTION_INVALID);
        }
        CardActionHandler handler = handlersByKey.get(actionKey.trim());
        if (handler == null) {
            throw new ServiceException(ErrorCodeConstants.CARD_ACTION_HANDLER_NOT_FOUND);
        }
        return handler;
    }
}
