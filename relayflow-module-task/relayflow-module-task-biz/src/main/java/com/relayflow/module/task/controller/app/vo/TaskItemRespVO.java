package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskItemRespVO {

    private Long id;

    private String title;

    private String status;

    private OffsetDateTime startTime;

    private OffsetDateTime dueTime;

    private Integer remindBeforeMinutes;

    private String description;

    private Long parentId;

    private Long listId;

    private Integer boardRank;

    private Long assigneeId;

    private Long creatorId;

    private Long assignerId;

    private OffsetDateTime createTime;

    private Integer subtaskDoneCount;

    private Integer subtaskTotal;
}
