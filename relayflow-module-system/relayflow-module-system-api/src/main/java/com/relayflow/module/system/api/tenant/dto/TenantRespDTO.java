package com.relayflow.module.system.api.tenant.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TenantRespDTO {

    private Long id;
    private String code;
    private String name;
    private Integer status;
    private OffsetDateTime createTime;
}
