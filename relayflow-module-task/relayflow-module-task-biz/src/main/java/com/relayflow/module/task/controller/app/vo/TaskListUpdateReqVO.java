package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskListUpdateReqVO {

    @NotNull
    private Long id;

    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;
}
