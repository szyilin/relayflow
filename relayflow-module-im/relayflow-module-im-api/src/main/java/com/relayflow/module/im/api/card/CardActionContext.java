package com.relayflow.module.im.api.card;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CardActionContext {

    private Long tenantId;
    private Long userId;
    private String botCode;
    private Long messageId;
    private Long conversationId;
    private String actionId;
    private String actionKey;
    private Map<String, Object> payload;
    private Map<String, Object> formValues;
    private String clientActionId;
}
