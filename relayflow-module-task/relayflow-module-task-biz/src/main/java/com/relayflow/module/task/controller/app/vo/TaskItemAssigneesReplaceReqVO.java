package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskItemAssigneesReplaceReqVO {

    @NotNull
    private Long id;

    /** Full replacement set; null treated as empty. */
    private List<Long> assigneeIds = new ArrayList<>();
}
