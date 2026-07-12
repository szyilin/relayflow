package com.relayflow.module.system.controller.admin.user.vo;

import com.relayflow.common.util.MobileUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UserInviteReqVO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入 11 位手机号")
    private String mobile;

    public void setMobile(String mobile) {
        this.mobile = MobileUtils.normalize(mobile);
    }

    private String nickname;
    private String email;
    private Long deptId;
    private List<Long> roleIds;
}
