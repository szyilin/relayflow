package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CardActionReqVO {

    @NotNull
    private Long messageId;

    @NotNull
    private Long conversationId;

    @NotBlank
    private String actionId;

    @NotBlank
    private String actionKey;

    private Map<String, Object> payload;

    private Map<String, Object> formValues;

    @NotBlank
    private String clientActionId;
}
