package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskItemBoardMoveReqVO {

    @NotNull
    private Long id;

    /** TODO | IN_PROGRESS | DONE */
    @NotBlank
    private String status;

    /** Optional column rank; null = append to end of target column. */
    private Integer boardRank;
}
