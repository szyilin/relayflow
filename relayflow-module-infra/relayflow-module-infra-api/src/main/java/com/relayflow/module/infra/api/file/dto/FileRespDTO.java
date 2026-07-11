package com.relayflow.module.infra.api.file.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FileRespDTO {

    private Long id;
    private Long tenantId;
    private String provider;
    private String storageUri;
    private String objectKey;
    private String originalName;
    private String mimeType;
    private Long size;
    private String sha256;
    private String accessLevel;
    private OffsetDateTime createTime;
}
