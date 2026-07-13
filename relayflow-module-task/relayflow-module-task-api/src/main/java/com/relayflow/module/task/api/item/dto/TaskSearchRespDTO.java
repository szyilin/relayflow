package com.relayflow.module.task.api.item.dto;

import lombok.Data;

@Data
public class TaskSearchRespDTO {

    private Long taskId;
    private String title;
    private String status;
}
