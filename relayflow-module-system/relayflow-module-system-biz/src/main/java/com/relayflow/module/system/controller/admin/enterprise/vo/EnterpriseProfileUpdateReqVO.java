package com.relayflow.module.system.controller.admin.enterprise.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnterpriseProfileUpdateReqVO {

    @NotBlank(message = "企业名称不能为空")
    private String name;

    private String industry;
    private String scale;
    /** infra file id as string; null/blank clears logo */
    private String logoFileId;
    private String contactName;
    private String contactEmail;
    private String contactMobile;
    private String contactLandline;
}
