package com.relayflow.module.im.service.message;

/**
 * User-facing message list window parameters and clamps (app-api /message/list).
 */
public final class ImMessageListWindows {

    public static final int DEFAULT_LIMIT = 50;
    public static final int HARD_CAP = 50;
    public static final int DEFAULT_AROUND_BEFORE = 25;
    public static final int DEFAULT_AROUND_AFTER = 25;

    public enum Mode {
        /** Highest seq side. */
        LATEST,
        /** Around anchor seq (includes the message at aroundSeq when present). */
        AROUND,
        /** Strictly before beforeSeq. */
        BEFORE,
        /** Strictly after afterSeq. */
        AFTER
    }

    private ImMessageListWindows() {
    }

    public static Mode resolveMode(Long afterSeq, Long beforeSeq, Long aroundSeq) {
        if (aroundSeq != null && aroundSeq > 0) {
            return Mode.AROUND;
        }
        if (beforeSeq != null && beforeSeq > 0) {
            return Mode.BEFORE;
        }
        if (afterSeq != null && afterSeq > 0) {
            return Mode.AFTER;
        }
        return Mode.LATEST;
    }

    public static int clampLimit(Integer limit) {
        int value = limit != null && limit > 0 ? limit : DEFAULT_LIMIT;
        return Math.min(value, HARD_CAP);
    }

    public static int clampAroundSide(Integer requested, int defaultValue) {
        int value = requested != null && requested > 0 ? requested : defaultValue;
        return Math.min(value, HARD_CAP);
    }
}
