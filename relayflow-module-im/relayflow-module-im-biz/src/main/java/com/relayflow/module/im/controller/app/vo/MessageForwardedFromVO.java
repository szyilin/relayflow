package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class MessageForwardedFromVO {

    private Long conversationId;
    private Long messageId;
    private String senderNickname;
    private String preview;
}
