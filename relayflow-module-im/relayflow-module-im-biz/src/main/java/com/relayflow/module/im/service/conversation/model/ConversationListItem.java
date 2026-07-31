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
    /** Peer / bot / group avatar file id (string), nullable. */
    private String avatar;
    private String lastMsgPreview;
    private OffsetDateTime lastMsgAt;
    private Integer unreadCount;
    /** Peer-visible read watermark (optional for clients that distinguish inbox vs receipt). */
    private Long readSeq;
    /** Inbox badge watermark; first unread seq is badgeSeq + 1 when unreadCount &gt; 0. */
    private Long badgeSeq;
    /** True when this member has unread user @mentions. */
    private Boolean mentionedMe;
    private Boolean pinned;
    private OffsetDateTime pinnedAt;
    private Boolean flagged;
    /** True when muteUntil is in the future. */
    private Boolean muted;
    private OffsetDateTime muteUntil;
    /** Current user's private tag ids on this conversation. */
    private java.util.List<Long> tagIds;
    private Long peerUserId;
    private Integer memberCount;
    private Long botId;
    private String botCode;
}
