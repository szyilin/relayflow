package com.relayflow.module.im.service.message;

/**
 * Materialize inbox unread from badge watermark (contiguous conversation seq).
 */
public final class ImMemberUnreadMath {

    private ImMemberUnreadMath() {
    }

    public static int unreadFromBadge(long maxSeq, Long badgeSeq) {
        long badge = badgeSeq != null ? badgeSeq : 0L;
        return (int) Math.max(0L, maxSeq - badge);
    }
}
