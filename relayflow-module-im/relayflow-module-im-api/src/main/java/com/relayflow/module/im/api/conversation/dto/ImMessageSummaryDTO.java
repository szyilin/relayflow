package com.relayflow.module.im.api.conversation.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ImMessageSummaryDTO {

    private Long id;
    private Long conversationId;
    private Long seq;
    private String senderType;
    private Long senderId;
    private String senderName;
    private String textPreview;
    private OffsetDateTime createTime;
    private Long replyToMsgId;
}
