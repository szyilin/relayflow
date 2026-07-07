package com.relayflow.module.system.controller.admin.user.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateStatusReqVO {

    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    /** 0=启用 1=禁用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
