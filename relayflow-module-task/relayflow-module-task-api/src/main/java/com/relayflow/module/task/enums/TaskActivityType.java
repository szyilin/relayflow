package com.relayflow.module.task.enums;

/**
 * Values for {@code task_activity.type}.
 */
public final class TaskActivityType {

    public static final String CREATED = "created";
    public static final String FIELD_CHANGED = "field_changed";
    public static final String SUBTASK_CREATED = "subtask_created";
    public static final String SUBTASK_DONE = "subtask_done";
    public static final String FOLLOWER_ADDED = "follower_added";
    public static final String FOLLOWER_REMOVED = "follower_removed";
    public static final String COMMENTED = "commented";
    public static final String ASSIGNED = "assigned";

    private TaskActivityType() {
    }
}
