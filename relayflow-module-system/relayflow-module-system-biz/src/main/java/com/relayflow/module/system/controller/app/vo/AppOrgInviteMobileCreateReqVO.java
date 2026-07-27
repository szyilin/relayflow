package com.relayflow.module.system.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppOrgInviteMobileCreateReqVO {

    @NotBlank(message = "手机号不能为空")
    private String mobile;
}
