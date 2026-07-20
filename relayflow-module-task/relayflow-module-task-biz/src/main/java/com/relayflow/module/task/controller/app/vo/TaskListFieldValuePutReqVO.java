package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListFieldValuePutReqVO {

    @NotNull
    private Long listId;

    @NotNull
    private Long itemId;

    @NotNull
    private Long fieldId;

    /** Null clears to 无分组 */
    private Long optionId;
}
