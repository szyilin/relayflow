package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskViewConfigSaveReqVO {

    @NotBlank
    private String contextType;

    /** Required when contextType=LIST */
    private Long contextId;

    @NotNull
    private TaskViewConfigVO config;
}
