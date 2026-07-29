package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class MessageReplyToRespVO {

    private Long messageId;
    private Long seq;
    private String senderType;
    private Long senderId;
    private String senderNickname;
    private String preview;
}
