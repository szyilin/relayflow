package com.relayflow.module.system.controller.admin.user.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateReqVO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String nickname;
    private String mobile;
    private String email;
    private Long deptId;
    private List<Long> roleIds;
}
