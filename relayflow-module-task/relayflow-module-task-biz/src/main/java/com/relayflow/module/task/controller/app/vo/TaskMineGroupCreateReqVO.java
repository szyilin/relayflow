package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskMineGroupCreateReqVO {

    @NotBlank
    private String name;
}
