package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskCommentCreateReqVO {

    @NotNull
    private Long taskId;

    @NotBlank
    @Size(max = 4000)
    private String content;
}
