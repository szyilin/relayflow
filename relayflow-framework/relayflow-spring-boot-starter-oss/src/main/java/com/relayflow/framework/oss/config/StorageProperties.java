package com.relayflow.framework.oss.config;

import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "relayflow.storage")
public class StorageProperties {

    /**
     * Required bootstrap default provider (V1: minio).
     */
    private ObjectStorageProviderType defaultProvider;

    /**
     * Allow {@link ObjectStorageProviderType#LOCAL} when dev profile is active.
     */
    private boolean allowLocal = false;

    /**
     * When true, verify default provider connectivity during startup.
     */
    private boolean validateOnStartup = true;

    private MinioProperties minio = new MinioProperties();

    public StorageProviderConfig toBootstrapConfig() {
        if (defaultProvider == null) {
            throw new IllegalStateException("relayflow.storage.default-provider is required");
        }
        return switch (defaultProvider) {
            case MINIO -> StorageProviderConfig.builder()
                    .providerType(ObjectStorageProviderType.MINIO)
                    .endpoint(minio.getEndpoint())
                    .accessKey(minio.getAccessKey())
                    .secretKey(minio.getSecretKey())
                    .bucket(minio.getBucket())
                    .useSsl(minio.isUseSsl())
                    .pathPrefix(normalizePathPrefix(minio.getPathPrefix()))
                    .build();
            case LOCAL, S3, OSS, COS -> throw new IllegalStateException(
                    "Bootstrap storage config is not defined for provider: " + defaultProvider);
        };
    }

    public void validateBootstrapBlocks() {
        if (defaultProvider == null) {
            throw new IllegalStateException("relayflow.storage.default-provider is required");
        }
        if (defaultProvider == ObjectStorageProviderType.MINIO) {
            validateMinioBlock();
        }
    }

    private void validateMinioBlock() {
        if (isBlank(minio.getEndpoint())) {
            throw new IllegalStateException("relayflow.storage.minio.endpoint is required");
        }
        if (isBlank(minio.getAccessKey())) {
            throw new IllegalStateException("relayflow.storage.minio.access-key is required");
        }
        if (isBlank(minio.getSecretKey())) {
            throw new IllegalStateException("relayflow.storage.minio.secret-key is required");
        }
        if (isBlank(minio.getBucket())) {
            throw new IllegalStateException("relayflow.storage.minio.bucket is required");
        }
    }

    private String normalizePathPrefix(String pathPrefix) {
        if (isBlank(pathPrefix)) {
            return "";
        }
        return pathPrefix;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Data
    public static class MinioProperties {

        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private boolean useSsl;
        private String pathPrefix = "";
    }
}
