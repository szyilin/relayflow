package com.relayflow.module.system.controller.admin.role.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleMemberRemoveReqVO {

    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @NotNull(message = "用户编号不能为空")
    private Long userId;
}
