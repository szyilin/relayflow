package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForwardMessageReqVO {

    @NotNull
    private Long messageId;

    @NotNull
    private Long targetConversationId;

    private String clientMsgId;
}
