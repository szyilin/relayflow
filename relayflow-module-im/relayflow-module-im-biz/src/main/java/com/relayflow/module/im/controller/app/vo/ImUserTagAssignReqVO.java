package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ImUserTagAssignReqVO {

    @NotNull
    private Long conversationId;

    @NotNull
    private Long tagId;
}
