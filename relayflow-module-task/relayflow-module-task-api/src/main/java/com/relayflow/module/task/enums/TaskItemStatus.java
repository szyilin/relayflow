package com.relayflow.module.task.enums;

public final class TaskItemStatus {

    public static final String TODO = "TODO";

    public static final String IN_PROGRESS = "IN_PROGRESS";

    public static final String DONE = "DONE";

    private TaskItemStatus() {
    }

    public static boolean isValid(String status) {
        return TODO.equals(status) || IN_PROGRESS.equals(status) || DONE.equals(status);
    }

    /** Incomplete for due-range / due Bot (not DONE). */
    public static boolean isOpen(String status) {
        return TODO.equals(status) || IN_PROGRESS.equals(status);
    }
}
