package com.relayflow.module.task.enums;

/**
 * Task list member roles.
 */
public final class TaskListRole {

    public static final String OWNER = "OWNER";
    public static final String EDITOR = "EDITOR";
    public static final String VIEWER = "VIEWER";

    private TaskListRole() {
    }

    public static boolean isOwner(String role) {
        return OWNER.equals(role);
    }

    public static boolean canEditMeta(String role) {
        return isOwner(role);
    }

    public static boolean canMutateTasks(String role) {
        return OWNER.equals(role) || EDITOR.equals(role);
    }

    public static boolean isValidInviteRole(String role) {
        return EDITOR.equals(role) || VIEWER.equals(role);
    }
}
