package com.relayflow.module.infra.controller.admin.storage.vo;
import lombok.Data;
@Data
public class StorageRevisionRespVO {
    private Long id;
    private String source;
    private String endpoint;
    private String bucket;
    private String region;
    private String pathPrefix;
    private boolean legacyRelativeKeys;
    private long fileReferences;
    private long sessionReferences;
}
