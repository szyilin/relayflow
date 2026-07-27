package com.relayflow.module.system.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppContactRequestCreateReqVO {

    @NotNull(message = "目标用户不能为空")
    private Long targetUserId;

    private String message;
}
