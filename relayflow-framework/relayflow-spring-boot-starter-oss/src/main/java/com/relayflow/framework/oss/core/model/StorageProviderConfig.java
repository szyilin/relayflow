package com.relayflow.framework.oss.core.model;

import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import lombok.Builder;
import lombok.Data;

/**
 * Runtime storage provider settings, decoupled from DB JSON or Spring properties shape.
 */
@Data
@Builder
public class StorageProviderConfig {

    private ObjectStorageProviderType providerType;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private boolean useSsl;
    private String pathPrefix;
}
