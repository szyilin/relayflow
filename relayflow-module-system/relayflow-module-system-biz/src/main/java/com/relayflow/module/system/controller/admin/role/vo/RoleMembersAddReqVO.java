package com.relayflow.module.system.controller.admin.role.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleMembersAddReqVO {

    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @NotEmpty(message = "用户列表不能为空")
    private List<Long> userIds;
}
