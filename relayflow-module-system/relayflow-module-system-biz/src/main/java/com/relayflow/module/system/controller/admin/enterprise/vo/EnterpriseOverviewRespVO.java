package com.relayflow.module.system.controller.admin.enterprise.vo;

import lombok.Data;

@Data
public class EnterpriseOverviewRespVO {

    private String code;
    private String name;
    /** infra file id as string; null when unset */
    private String logoFileId;
    private Long memberCount;
    private Long deptCount;
    private Long superAdminCount;
    private Long subAdminCount;
}
