package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListFieldOptionUpdateReqVO {

    @NotNull
    private Long id;

    private String label;

    private Integer rank;

    private String color;
}
