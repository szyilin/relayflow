package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskFollowReqVO {

    @NotNull
    private Long taskId;
}
