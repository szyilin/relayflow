package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListGroupCreateReqVO {

    @NotNull
    private Long listId;

    @NotBlank
    private String name;
}
