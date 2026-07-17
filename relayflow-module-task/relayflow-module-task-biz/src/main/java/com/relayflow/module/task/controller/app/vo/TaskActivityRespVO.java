package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskActivityRespVO {

    private Long id;

    private Long taskId;

    private String taskTitle;

    private Long actorId;

    private String actorNickname;

    private String type;

    private String summary;

    private OffsetDateTime createTime;
}
