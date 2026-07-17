package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskListRespVO {

    private Long id;

    private String name;

    private String description;

    private Long ownerId;

    private Boolean archived;

    private String myRole;

    private OffsetDateTime createTime;
}
