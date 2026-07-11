package com.relayflow.framework.oss.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StorageObjectMeta {

    private String objectKey;
    private long size;
    private String etag;
    private String contentType;
    private Instant lastModified;
}
