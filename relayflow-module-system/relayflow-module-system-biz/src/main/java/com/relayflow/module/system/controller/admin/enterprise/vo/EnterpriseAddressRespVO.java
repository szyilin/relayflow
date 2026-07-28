package com.relayflow.module.system.controller.admin.enterprise.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class EnterpriseAddressRespVO {

    private Long id;
    private List<String> types;
    private Long regionId;
    private String regionPath;
    private String detail;
    private String status;
    private OffsetDateTime createTime;
}
