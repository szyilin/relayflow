package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListMemberUpdateRoleReqVO {

    @NotNull
    private Long listId;

    @NotNull
    private Long userId;

    @NotBlank
    private String role;
}
