package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageReqVO {

    private Long conversationId;
    private Long peerUserId;

    @NotBlank
    private String clientMsgId;

    private String type;

    /** Optional single-message quote (WeChat-style reply_to). */
    private Long replyToMsgId;

    @NotNull
    @Valid
    private MessageContentVO content;
}
