package com.relayflow.module.system.controller.admin.invite.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InviteSettingsRespVO {

    @NotBlank
    private String inviteMode;

    @NotBlank
    private String nonAdminScope;

    @NotNull
    private List<String> nonAdminDeptIds = new ArrayList<>();

    @NotNull
    private Integer nonAdminExpireDays;

    @NotNull
    private Boolean nonAdminRequireApproval;
}
