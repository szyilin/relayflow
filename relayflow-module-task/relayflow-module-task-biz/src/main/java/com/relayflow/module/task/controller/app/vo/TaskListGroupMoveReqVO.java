package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListGroupMoveReqVO {

    @NotNull
    private Long listId;

    @NotNull
    private Long taskId;

    @NotNull
    private Long groupId;

    private Long beforeId;
}
