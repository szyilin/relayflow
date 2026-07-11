package com.relayflow.framework.oss.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class PresignedUpload {

    private String uploadUrl;
    private Map<String, String> headers;
    private Instant expiresAt;
}
