package com.relayflow.framework.oss.core.model;

import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import lombok.Builder;
import lombok.Data;

/**
 * Runtime storage provider settings, decoupled from DB JSON or Spring properties shape.
 */
@Data
@Builder(toBuilder = true)
public class StorageProviderConfig {

    private ObjectStorageProviderType providerType;
    private String endpoint;
    private String publicEndpoint;
    private String region;
    private String accessKey;
    @lombok.ToString.Exclude
    private String secretKey;
    private String bucket;
    private boolean useSsl;
    private String pathPrefix;
}
