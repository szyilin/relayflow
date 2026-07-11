package com.relayflow.module.infra.controller.admin.file.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class FileUploadSessionRespVO {

    private Long uploadId;
    private String mode;
    private String objectKey;
    private String uploadUrl;
    private Map<String, String> headers;
    private OffsetDateTime expiresAt;
}
