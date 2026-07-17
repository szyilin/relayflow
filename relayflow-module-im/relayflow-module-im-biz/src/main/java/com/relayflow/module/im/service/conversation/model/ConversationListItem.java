package com.relayflow.module.im.service.conversation.model;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Internal conversation list row — not an HTTP VO.
 */
@Data
public class ConversationListItem {

    private Long id;
    private String type;
    private String title;
    private String avatarText;
    private String lastMsgPreview;
    private OffsetDateTime lastMsgAt;
    private Integer unreadCount;
    private Long peerUserId;
    private Integer memberCount;
    private Long botId;
    private String botCode;
}
