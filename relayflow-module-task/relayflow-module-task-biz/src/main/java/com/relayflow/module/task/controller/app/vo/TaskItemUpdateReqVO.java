package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskItemUpdateReqVO {

    @NotNull
    private Long id;

    @Size(max = 200)
    private String title;

    private OffsetDateTime dueTime;
}
