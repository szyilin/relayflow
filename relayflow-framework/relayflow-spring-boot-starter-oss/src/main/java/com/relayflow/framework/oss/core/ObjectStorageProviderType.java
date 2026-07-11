package com.relayflow.framework.oss.core;

/**
 * Supported object storage backends. V1 only {@link #MINIO} has a client implementation.
 */
public enum ObjectStorageProviderType {

    MINIO,
    S3,
    OSS,
    COS,
    LOCAL
}
