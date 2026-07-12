package com.relayflow.module.system.controller.app.vo;

import com.relayflow.common.util.MobileUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemberInviteAcceptReqVO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入 11 位手机号")
    private String mobile;

    public void setMobile(String mobile) {
        this.mobile = MobileUtils.normalize(mobile);
    }

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少 6 位")
    private String password;
}
