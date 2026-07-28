package com.relayflow.module.task.enums;

/**
 * Task domain string constants shared by API / biz.
 */
public final class TaskConstants {

    /** Detail get: task is readable. */
    public static final String ACCESS_STATE_OK = "OK";

    /** Detail get: task row exists but was soft-deleted (tombstone). */
    public static final String ACCESS_STATE_DELETED = "DELETED";

    private TaskConstants() {
    }
}
