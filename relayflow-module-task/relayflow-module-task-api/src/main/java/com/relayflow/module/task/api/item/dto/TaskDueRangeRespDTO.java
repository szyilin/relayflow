package com.relayflow.module.task.api.item.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskDueRangeRespDTO {

    private Long taskId;

    private String title;

    private String status;

    private OffsetDateTime dueTime;
}
