package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SendMessageRespVO {

    private Long id;
    private Long conversationId;
    private Long seq;
    private String clientMsgId;
    private OffsetDateTime createTime;
}
