package com.relayflow.module.infra.controller.admin.storage.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorageTestConnectionReqVO {

    @NotBlank
    private String provider;

    private String endpoint;

    private String bucket;

    private String accessKey;

    private String secretKey;

    private Boolean useSsl;

    private String pathPrefix;
}
