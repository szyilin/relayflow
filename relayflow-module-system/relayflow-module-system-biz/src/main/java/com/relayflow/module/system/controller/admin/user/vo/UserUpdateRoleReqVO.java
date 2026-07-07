package com.relayflow.module.system.controller.admin.user.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRoleReqVO {

    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    private List<Long> roleIds;
}
