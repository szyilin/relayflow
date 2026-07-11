package com.relayflow.module.infra.controller.admin.storage.vo;

import lombok.Data;

@Data
public class StorageProviderRespVO {

    private String provider;

    private String status;

    private Boolean isDefault;

    private String endpoint;

    private String bucket;

    private String accessKey;

    private Boolean useSsl;

    private String pathPrefix;

    /**
     * Whether a secret key is configured (never expose plaintext).
     */
    private Boolean secretKeyConfigured;
}
