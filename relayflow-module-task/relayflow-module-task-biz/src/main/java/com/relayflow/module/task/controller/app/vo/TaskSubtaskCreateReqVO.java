package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskSubtaskCreateReqVO {

    @NotNull
    private Long parentId;

    @NotBlank
    @Size(max = 200)
    private String title;
}
