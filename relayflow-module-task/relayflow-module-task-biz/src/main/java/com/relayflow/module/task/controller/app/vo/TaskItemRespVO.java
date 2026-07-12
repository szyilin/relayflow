package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskItemRespVO {

    private Long id;

    private String title;

    private String status;

    private OffsetDateTime dueTime;

    private OffsetDateTime createTime;
}
