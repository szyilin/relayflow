package com.relayflow.module.agent.skill.task;

import lombok.Data;

@Data
public class TaskSearchInput {

    /** Keyword; empty means recent/broad match per TaskItemApi. */
    private String keyword;

    /** Max rows; default 10, capped at 50. */
    private Integer limit;
}
