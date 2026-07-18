package com.relayflow.module.task.enums;

import java.util.Set;

public final class TaskViewContextType {

    public static final String MINE = "MINE";
    public static final String FOLLOWING = "FOLLOWING";
    public static final String ALL = "ALL";
    public static final String CREATED = "CREATED";
    public static final String ASSIGNED_BY_ME = "ASSIGNED_BY_ME";
    public static final String COMPLETED = "COMPLETED";
    public static final String LIST = "LIST";

    private static final Set<String> PERSONAL = Set.of(
            MINE, FOLLOWING, ALL, CREATED, ASSIGNED_BY_ME, COMPLETED);

    private TaskViewContextType() {
    }

    public static boolean isPersonal(String type) {
        return type != null && PERSONAL.contains(type.trim().toUpperCase());
    }

    public static boolean isList(String type) {
        return LIST.equalsIgnoreCase(type == null ? "" : type.trim());
    }

    public static boolean isValid(String type) {
        return isPersonal(type) || isList(type);
    }

    public static String normalize(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }
}
