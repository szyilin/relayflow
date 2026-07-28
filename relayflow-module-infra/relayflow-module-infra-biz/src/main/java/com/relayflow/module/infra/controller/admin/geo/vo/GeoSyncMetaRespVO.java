package com.relayflow.module.infra.controller.admin.geo.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class GeoSyncMetaRespVO {

    private String source;
    private String dataVersion;
    private String status;
    private Integer regionCount;
    private String message;
    private OffsetDateTime syncedAt;
}
