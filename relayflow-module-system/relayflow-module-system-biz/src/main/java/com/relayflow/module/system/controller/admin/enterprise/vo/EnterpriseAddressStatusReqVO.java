package com.relayflow.module.system.controller.admin.enterprise.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnterpriseAddressStatusReqVO {

    @NotBlank(message = "状态不能为空")
    private String status;
}
