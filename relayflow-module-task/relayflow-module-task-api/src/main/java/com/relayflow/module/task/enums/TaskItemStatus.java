package com.relayflow.module.task.enums;

public final class TaskItemStatus {

    /** Incomplete (unchecked). */
    public static final String TODO = "TODO";

    /** Complete (checked). */
    public static final String DONE = "DONE";

    private TaskItemStatus() {
    }

    public static boolean isValid(String status) {
        return TODO.equals(status) || DONE.equals(status);
    }

    /** Incomplete for due-range / due Bot (not DONE). */
    public static boolean isOpen(String status) {
        return TODO.equals(status);
    }
}
