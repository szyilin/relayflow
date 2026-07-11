package com.relayflow.framework.oss.minio;

import com.relayflow.framework.oss.core.ObjectStorageClient;
import com.relayflow.framework.oss.core.model.PresignedUpload;
import com.relayflow.framework.oss.core.model.StorageObjectMeta;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class MinioObjectStorageClient implements ObjectStorageClient {

    @Override
    public void checkConnectivity(StorageProviderConfig config) {
        try {
            MinioClient client = buildClient(config);
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(requireBucket(config))
                    .build());
            if (!exists) {
                throw new IllegalStateException("MinIO bucket does not exist: " + config.getBucket());
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("MinIO connectivity check failed for bucket " + config.getBucket(), ex);
        }
    }

    @Override
    public PresignedUpload createPresignedPut(StorageProviderConfig config, String objectKey, String contentType, Duration ttl) {
        try {
            MinioClient client = buildClient(config);
            String resolvedKey = resolveObjectKey(config, objectKey);
            int expirySeconds = toExpirySeconds(ttl);
            String uploadUrl = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(requireBucket(config))
                    .object(resolvedKey)
                    .expiry(expirySeconds)
                    .build());
            Instant expiresAt = Instant.now().plusSeconds(expirySeconds);
            return PresignedUpload.builder()
                    .uploadUrl(uploadUrl)
                    .headers(Map.of("Content-Type", contentType))
                    .expiresAt(expiresAt)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create MinIO presigned PUT for object " + objectKey, ex);
        }
    }

    @Override
    public StorageObjectMeta headObject(StorageProviderConfig config, String objectKey) {
        try {
            MinioClient client = buildClient(config);
            String resolvedKey = resolveObjectKey(config, objectKey);
            var stat = client.statObject(StatObjectArgs.builder()
                    .bucket(requireBucket(config))
                    .object(resolvedKey)
                    .build());
            return StorageObjectMeta.builder()
                    .objectKey(resolvedKey)
                    .size(stat.size())
                    .etag(stat.etag())
                    .contentType(stat.contentType())
                    .lastModified(stat.lastModified() != null ? stat.lastModified().toInstant() : null)
                    .build();
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code())) {
                return null;
            }
            throw new IllegalStateException("Failed to head MinIO object " + objectKey, ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to head MinIO object " + objectKey, ex);
        }
    }

    @Override
    public void deleteObject(StorageProviderConfig config, String objectKey) {
        try {
            MinioClient client = buildClient(config);
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(requireBucket(config))
                    .object(resolveObjectKey(config, objectKey))
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete MinIO object " + objectKey, ex);
        }
    }

    @Override
    public String createPresignedGet(StorageProviderConfig config, String objectKey, Duration ttl) {
        try {
            MinioClient client = buildClient(config);
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(requireBucket(config))
                    .object(resolveObjectKey(config, objectKey))
                    .expiry(toExpirySeconds(ttl))
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create MinIO presigned GET for object " + objectKey, ex);
        }
    }

    private MinioClient buildClient(StorageProviderConfig config) {
        validateConfig(config);
        return MinioClient.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
    }

    private void validateConfig(StorageProviderConfig config) {
        if (isBlank(config.getEndpoint())) {
            throw new IllegalStateException("MinIO endpoint is required");
        }
        if (isBlank(config.getAccessKey()) || isBlank(config.getSecretKey())) {
            throw new IllegalStateException("MinIO credentials are required");
        }
        requireBucket(config);
    }

    private String requireBucket(StorageProviderConfig config) {
        if (isBlank(config.getBucket())) {
            throw new IllegalStateException("MinIO bucket is required");
        }
        return config.getBucket();
    }

    private String resolveObjectKey(StorageProviderConfig config, String objectKey) {
        String prefix = config.getPathPrefix();
        if (isBlank(prefix)) {
            return objectKey;
        }
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return normalizedPrefix + objectKey;
    }

    private int toExpirySeconds(Duration ttl) {
        long seconds = ttl.getSeconds();
        if (seconds <= 0) {
            throw new IllegalArgumentException("Presigned URL TTL must be positive");
        }
        if (seconds > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) seconds;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
