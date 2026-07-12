package com.relayflow.module.system.controller.admin.auth.vo;

import com.relayflow.common.util.MobileUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AuthLoginReqVO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入 11 位手机号")
    private String username;

    public void setUsername(String username) {
        this.username = MobileUtils.normalize(username);
    }

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 多企业登录时指定目标 tenant；`enabled=true` 且未指定且有多企业时返回 TENANT_SELECTION_REQUIRED */
    private Long tenantId;
}
