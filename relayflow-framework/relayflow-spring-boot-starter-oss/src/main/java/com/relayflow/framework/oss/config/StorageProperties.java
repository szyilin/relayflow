package com.relayflow.framework.oss.config;

import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Locale;

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
            case MINIO -> resolveRuntimeConfig(StorageProviderConfig.builder()
                    .providerType(ObjectStorageProviderType.MINIO)
                    .endpoint(minio.getEndpoint())
                    .publicEndpoint(blankToNull(minio.getPublicEndpoint()))
                    .region(blankToNull(minio.getRegion()))
                    .accessKey(minio.getAccessKey())
                    .secretKey(minio.getSecretKey())
                    .bucket(minio.getBucket())
                    .useSsl(minio.isUseSsl())
                    .pathPrefix(normalizePathPrefix(minio.getPathPrefix()))
                    .build());
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

    /** Apply deployment addresses only to this deployment's managed MinIO. */
    public StorageProviderConfig resolveRuntimeConfig(StorageProviderConfig config) {
        if (config.getProviderType() != ObjectStorageProviderType.MINIO || isBlank(minio.getPublicEndpoint())) {
            return config;
        }
        validateManagedAddresses();
        String endpoint = normalizeEndpoint(config.getEndpoint());
        if (endpoint == null || (!endpoint.equals(normalizeEndpoint(minio.getEndpoint()))
                && !endpoint.equals(normalizeEndpoint(minio.getPublicEndpoint())))) {
            return config;
        }
        return config.toBuilder()
                .endpoint(minio.getEndpoint().trim())
                .publicEndpoint(minio.getPublicEndpoint().trim())
                .region(minio.getRegion().trim())
                .build();
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
        if (!isBlank(minio.getPublicEndpoint())) {
            validateManagedAddresses();
        }
    }

    private void validateManagedAddresses() {
        if (normalizeEndpoint(minio.getEndpoint()) == null || normalizeEndpoint(minio.getPublicEndpoint()) == null) {
            throw new IllegalStateException("MinIO internal/public endpoints must be HTTP(S) origins without path or credentials");
        }
        if (isBlank(minio.getRegion())) {
            throw new IllegalStateException("relayflow.storage.minio.region is required with public-endpoint");
        }
    }

    private String normalizeEndpoint(String endpoint) {
        if (isBlank(endpoint)) {
            return null;
        }
        try {
            URI uri = URI.create(endpoint.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !uri.getRawPath().equals("/"))) {
                return null;
            }
            int port = uri.getPort() == -1 ? (scheme.equals("https") ? 443 : 80) : uri.getPort();
            if (port < 1 || port > 65535) {
                return null;
            }
            return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
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
        /** Browser-facing signing origin; absent preserves the single endpoint behavior. */
        private String publicEndpoint;
        /** Required for dual-address signing to avoid network discovery on the public origin. */
        private String region;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private boolean useSsl;
        private String pathPrefix = "";
    }
}
