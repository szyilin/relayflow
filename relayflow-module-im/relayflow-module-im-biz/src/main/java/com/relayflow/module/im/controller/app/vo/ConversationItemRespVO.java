package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ConversationItemRespVO {

    private Long id;
    private String type;
    private String title;
    private String avatarText;
    private String lastMsgPreview;
    private OffsetDateTime lastMsgAt;
    private Integer unreadCount;
    private Long peerUserId;
    private Integer memberCount;
}
