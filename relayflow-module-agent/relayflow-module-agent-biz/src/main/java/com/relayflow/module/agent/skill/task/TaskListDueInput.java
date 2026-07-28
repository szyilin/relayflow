package com.relayflow.module.agent.skill.task;

import lombok.Data;

@Data
public class TaskListDueInput {

    /** Inclusive start of due-time range (ISO-8601 offset datetime). */
    private String from;

    /** Exclusive end of due-time range (ISO-8601 offset datetime). */
    private String to;

    /** Max rows; default 20, capped at 50. */
    private Integer limit;
}
