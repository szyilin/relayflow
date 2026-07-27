package com.relayflow.module.system.controller.admin.invite.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InviteSettingsUpdateReqVO {

    @NotBlank(message = "inviteMode 不能为空")
    private String inviteMode;

    @NotBlank(message = "nonAdminScope 不能为空")
    private String nonAdminScope;

    @NotNull(message = "nonAdminDeptIds 不能为空")
    private List<String> nonAdminDeptIds = new ArrayList<>();

    @NotNull(message = "nonAdminExpireDays 不能为空")
    private Integer nonAdminExpireDays;

    @NotNull(message = "nonAdminRequireApproval 不能为空")
    private Boolean nonAdminRequireApproval;
}
