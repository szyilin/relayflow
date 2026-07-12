package com.relayflow.module.system.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantSwitchReqVO {

    @NotNull(message = "企业 ID 不能为空")
    private Long tenantId;
}
