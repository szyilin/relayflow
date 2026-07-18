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

    /** Multi-list memberships (sorted by rank). */
    private java.util.List<Long> listIds;

    private Integer boardRank;

    private Long assigneeId;

    /** Multi-assignee ids (sorted ascending). */
    private java.util.List<Long> assigneeIds;

    private Long creatorId;

    private Long assignerId;

    private OffsetDateTime createTime;

    private Integer subtaskDoneCount;

    private Integer subtaskTotal;
}
