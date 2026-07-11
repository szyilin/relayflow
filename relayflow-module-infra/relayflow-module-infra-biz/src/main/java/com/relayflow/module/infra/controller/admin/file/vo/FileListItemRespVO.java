package com.relayflow.module.infra.controller.admin.file.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FileListItemRespVO {

    private Long id;
    private String originalName;
    private String mimeType;
    private Long size;
    private String accessLevel;
    private String provider;
    private String storageUri;
    private OffsetDateTime createTime;
}
