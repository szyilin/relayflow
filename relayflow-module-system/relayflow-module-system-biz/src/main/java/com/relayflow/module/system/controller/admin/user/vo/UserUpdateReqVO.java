package com.relayflow.module.system.controller.admin.user.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateReqVO {

    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    private String nickname;
    private String mobile;
    private String email;
}
