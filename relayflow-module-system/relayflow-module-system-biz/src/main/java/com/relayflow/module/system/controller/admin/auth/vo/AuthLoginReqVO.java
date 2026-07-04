package com.relayflow.module.system.controller.admin.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginReqVO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
