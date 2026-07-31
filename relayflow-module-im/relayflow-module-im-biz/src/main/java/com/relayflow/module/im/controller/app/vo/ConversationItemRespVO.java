package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ConversationItemRespVO {

    private Long id;
    private String type;
    private String title;
    private String avatarText;
    /** Peer / bot / group avatar file id (string), nullable. */
    private String avatar;
    private String lastMsgPreview;
    private OffsetDateTime lastMsgAt;
    private Integer unreadCount;
    /** True when the current user has unread @mentions in this conversation. */
    private Boolean mentionedMe;
    private Long peerUserId;
    private Integer memberCount;
    /** Present when {@code type=bot_dm}. */
    private Long botId;
    private String botCode;
}
