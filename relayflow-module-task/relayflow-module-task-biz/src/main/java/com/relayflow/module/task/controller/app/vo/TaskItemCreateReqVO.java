package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskItemCreateReqVO {

    @NotBlank
    @Size(max = 200)
    private String title;

    private OffsetDateTime dueTime;
}
