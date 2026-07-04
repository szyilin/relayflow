package com.relayflow.module.system.controller.admin.tenant.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TenantRespVO {

    private Long id;
    private String code;
    private String name;
    private Integer status;
    private OffsetDateTime createTime;
}
