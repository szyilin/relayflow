package com.relayflow.module.system.controller.app.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppUserProfileUpdateReqVO {

    @Size(max = 64, message = "昵称最多 64 个字符")
    private String nickname;

    @Size(max = 512, message = "头像标识无效")
    private String avatar;
}
