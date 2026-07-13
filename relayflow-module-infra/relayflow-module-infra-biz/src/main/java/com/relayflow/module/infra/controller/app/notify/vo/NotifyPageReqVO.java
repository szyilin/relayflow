package com.relayflow.module.infra.controller.app.notify.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class NotifyPageReqVO {

    @Min(1)
    private Integer pageNo = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    /** Optional notification type filter, e.g. TASK_DUE. */
    private String type;
}
