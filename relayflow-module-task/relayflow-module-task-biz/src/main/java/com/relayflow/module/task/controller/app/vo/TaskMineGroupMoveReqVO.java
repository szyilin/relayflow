package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskMineGroupMoveReqVO {

    @NotNull
    private Long taskId;

    @NotNull
    private Long groupId;

    /** Optional: insert before this task within the target group. */
    private Long beforeId;
}
