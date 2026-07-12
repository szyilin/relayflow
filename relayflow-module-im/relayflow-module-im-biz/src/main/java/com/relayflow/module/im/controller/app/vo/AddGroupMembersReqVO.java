package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddGroupMembersReqVO {

    @NotNull
    private Long conversationId;

    @NotEmpty
    private List<Long> memberUserIds;
}
