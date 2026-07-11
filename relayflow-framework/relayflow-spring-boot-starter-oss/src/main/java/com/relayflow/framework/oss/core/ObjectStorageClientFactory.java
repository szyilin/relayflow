package com.relayflow.framework.oss.core;

import com.relayflow.framework.oss.minio.MinioObjectStorageClient;

public class ObjectStorageClientFactory {

    public ObjectStorageClient getClient(ObjectStorageProviderType type) {
        return switch (type) {
            case MINIO -> new MinioObjectStorageClient();
            case S3, OSS, COS, LOCAL -> throw new UnsupportedStorageProviderException(type);
        };
    }
}
