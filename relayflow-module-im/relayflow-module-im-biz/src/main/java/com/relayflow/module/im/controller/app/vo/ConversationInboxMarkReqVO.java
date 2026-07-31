package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConversationInboxMarkReqVO {

    @NotNull
    private Long conversationId;
}
