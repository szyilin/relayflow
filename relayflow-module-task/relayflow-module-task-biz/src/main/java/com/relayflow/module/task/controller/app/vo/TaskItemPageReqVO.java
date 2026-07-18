package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TaskItemPageReqVO {

    @Min(1)
    private Integer pageNo = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    private String status;

    /**
     * ASSIGNEE (default) = 我负责的；CREATOR = 我创建的；
     * ALL = 可见并集；ASSIGNED_BY_ME = 我分配的。
     * Ignored when {@code listId} is set (list member page).
     */
    private String scope;

    /** When set, return root tasks of this list (member-scoped). */
    private Long listId;
}
