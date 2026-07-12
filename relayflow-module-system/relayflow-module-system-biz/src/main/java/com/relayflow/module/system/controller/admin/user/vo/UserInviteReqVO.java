package com.relayflow.module.system.controller.admin.user.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UserInviteReqVO {

    @NotBlank(message = "手机号不能为空")
    private String mobile;

    private String nickname;
    private String email;
    private Long deptId;
    private List<Long> roleIds;
}
