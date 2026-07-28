package com.relayflow.module.system.controller.admin.enterprise.vo;

import lombok.Data;

@Data
public class EnterpriseProfileRespVO {

    private String code;
    private String name;
    private String industry;
    private String scale;
    /** infra file id as string; null when unset */
    private String logoFileId;
    private String contactName;
    private String contactEmail;
    private String contactMobile;
    private String contactLandline;
}
