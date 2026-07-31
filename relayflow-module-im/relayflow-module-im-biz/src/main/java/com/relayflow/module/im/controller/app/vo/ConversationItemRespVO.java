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
    /** Peer-visible read watermark. */
    private Long readSeq;
    /** Inbox badge watermark; first unread seq is badgeSeq + 1 when unreadCount &gt; 0. */
    private Long badgeSeq;
    /** True when the current user has unread @mentions in this conversation. */
    private Boolean mentionedMe;
    private Boolean pinned;
    private OffsetDateTime pinnedAt;
    private Boolean flagged;
    private Boolean muted;
    private OffsetDateTime muteUntil;
    /** Current user's private tag ids on this conversation. */
    private java.util.List<Long> tagIds;
    private Long peerUserId;
    private Integer memberCount;
    /** Present when {@code type=bot_dm}. */
    private Long botId;
    private String botCode;
}
