package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskAssignReqVO {

    @NotNull
    private Long id;

    @NotNull
    private Long assigneeId;
}
