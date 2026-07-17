package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskCommentRespVO {

    private Long id;

    private Long taskId;

    private Long authorId;

    private String authorNickname;

    private String content;

    private OffsetDateTime createTime;
}
