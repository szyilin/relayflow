package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListGroupUpdateReqVO {

    @NotNull
    private Long id;

    private String name;

    private Integer rank;
}
