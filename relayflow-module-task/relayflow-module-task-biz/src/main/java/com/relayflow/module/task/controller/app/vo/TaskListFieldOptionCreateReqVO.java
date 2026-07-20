package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListFieldOptionCreateReqVO {

    @NotNull
    private Long fieldId;

    @NotBlank
    private String label;

    private String valueKey;

    private Integer rank;
}
