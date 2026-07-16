package com.relayflow.module.system.controller.app.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppContactRemarkUpdateReqVO {

    @Size(max = 64, message = "备注名最多 64 个字符")
    private String remarkName;

    @Size(max = 500, message = "描述最多 500 个字符")
    private String description;
}
