package com.relayflow.module.infra.controller.admin.storage.vo;

import lombok.Data;

@Data
public class StorageTestConnectionReqVO {

    /**
     * {@code bootstrap} tests deployment config; {@code tenant} tests saved or inline tenant config.
     */
    private String source;

    private String provider;

    private String endpoint;

    private String bucket;

    private String accessKey;

    private String secretKey;

    private Boolean useSsl;

    private String pathPrefix;
}
