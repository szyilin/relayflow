package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupBotMembershipReqVO {

    @NotNull
    private Long conversationId;

    @NotBlank
    private String botCode;
}
