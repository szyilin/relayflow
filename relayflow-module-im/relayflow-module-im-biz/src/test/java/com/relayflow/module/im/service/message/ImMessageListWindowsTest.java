package com.relayflow.module.im.service.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImMessageListWindowsTest {

    @Test
    void resolveModePrefersAroundThenBeforeThenAfterThenLatest() {
        assertEquals(ImMessageListWindows.Mode.AROUND,
                ImMessageListWindows.resolveMode(5L, 9L, 3L));
        assertEquals(ImMessageListWindows.Mode.BEFORE,
                ImMessageListWindows.resolveMode(5L, 9L, null));
        assertEquals(ImMessageListWindows.Mode.AFTER,
                ImMessageListWindows.resolveMode(5L, null, null));
        assertEquals(ImMessageListWindows.Mode.LATEST,
                ImMessageListWindows.resolveMode(0L, null, null));
        assertEquals(ImMessageListWindows.Mode.LATEST,
                ImMessageListWindows.resolveMode(null, null, null));
    }

    @Test
    void clampLimitHonorsDefaultAndHardCap() {
        assertEquals(ImMessageListWindows.DEFAULT_LIMIT, ImMessageListWindows.clampLimit(null));
        assertEquals(ImMessageListWindows.DEFAULT_LIMIT, ImMessageListWindows.clampLimit(0));
        assertEquals(10, ImMessageListWindows.clampLimit(10));
        assertEquals(ImMessageListWindows.HARD_CAP, ImMessageListWindows.clampLimit(999));
    }

    @Test
    void clampAroundSideUsesDefaultAndHardCap() {
        assertEquals(25, ImMessageListWindows.clampAroundSide(null, 25));
        assertEquals(25, ImMessageListWindows.clampAroundSide(0, 25));
        assertEquals(12, ImMessageListWindows.clampAroundSide(12, 25));
        assertEquals(ImMessageListWindows.HARD_CAP, ImMessageListWindows.clampAroundSide(100, 25));
    }

    @Test
    void unreadFromBadgeUsesMaxMinusBadge() {
        assertEquals(0, ImMemberUnreadMath.unreadFromBadge(10, 10L));
        assertEquals(1, ImMemberUnreadMath.unreadFromBadge(10, 9L));
        assertEquals(10, ImMemberUnreadMath.unreadFromBadge(10, null));
        assertEquals(0, ImMemberUnreadMath.unreadFromBadge(0, 0L));
        assertEquals(0, ImMemberUnreadMath.unreadFromBadge(3, 5L));
    }
}
