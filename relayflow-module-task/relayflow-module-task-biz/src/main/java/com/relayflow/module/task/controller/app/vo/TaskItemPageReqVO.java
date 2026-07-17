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
     * ASSIGNEE (default) = 我负责的；CREATOR = 我创建的。
     */
    private String scope;
}
