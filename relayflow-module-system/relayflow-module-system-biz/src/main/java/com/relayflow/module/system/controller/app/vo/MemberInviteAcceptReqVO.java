package com.relayflow.module.system.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemberInviteAcceptReqVO {

    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少 6 位")
    private String password;
}
