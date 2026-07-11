package com.relayflow.framework.oss.core;

import com.relayflow.framework.oss.core.model.PresignedUpload;
import com.relayflow.framework.oss.core.model.StorageObjectMeta;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;

import java.time.Duration;

public interface ObjectStorageClient {

    void checkConnectivity(StorageProviderConfig config);

    PresignedUpload createPresignedPut(StorageProviderConfig config, String objectKey, String contentType, Duration ttl);

    StorageObjectMeta headObject(StorageProviderConfig config, String objectKey);

    void deleteObject(StorageProviderConfig config, String objectKey);

    String createPresignedGet(StorageProviderConfig config, String objectKey, Duration ttl);
}
