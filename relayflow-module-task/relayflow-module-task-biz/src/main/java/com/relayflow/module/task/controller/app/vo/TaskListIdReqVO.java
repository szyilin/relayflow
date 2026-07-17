package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListIdReqVO {

    @NotNull
    private Long id;
}
