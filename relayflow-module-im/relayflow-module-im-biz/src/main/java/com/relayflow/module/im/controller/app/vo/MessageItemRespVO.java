package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessageItemRespVO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderNickname;
    private String senderType;
    private String type;
    private MessageContentVO content;
    private String clientMsgId;
    private Long seq;
    private OffsetDateTime createTime;
    private Boolean recalled;
    private Boolean edited;
    private OffsetDateTime recalledAt;
    private OffsetDateTime editedAt;
    /** Present when this message quotes another in the same conversation. */
    private MessageReplyToRespVO replyTo;
}
