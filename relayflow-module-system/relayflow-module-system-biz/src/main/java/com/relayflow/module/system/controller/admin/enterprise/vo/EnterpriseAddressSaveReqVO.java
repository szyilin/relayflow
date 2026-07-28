package com.relayflow.module.system.controller.admin.enterprise.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EnterpriseAddressSaveReqVO {

    @NotEmpty(message = "地址类型不能为空")
    private List<String> types;

    @NotNull(message = "行政区划不能为空")
    private Long regionId;

    @NotBlank(message = "区划路径不能为空")
    private String regionPath;

    @NotBlank(message = "详细地址不能为空")
    private String detail;
}
