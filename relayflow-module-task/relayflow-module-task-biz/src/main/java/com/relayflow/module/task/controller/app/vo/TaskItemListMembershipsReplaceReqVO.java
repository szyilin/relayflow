package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TaskItemListMembershipsReplaceReqVO {

    @NotNull
    private Long id;

    /** Full replace; null treated as empty. */
    private List<Long> listIds;
}
