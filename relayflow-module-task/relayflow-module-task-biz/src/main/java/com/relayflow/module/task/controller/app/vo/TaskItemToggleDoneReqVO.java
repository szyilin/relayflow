package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskItemToggleDoneReqVO {

    @NotNull
    private Long id;

    @NotNull
    private Boolean done;
}
