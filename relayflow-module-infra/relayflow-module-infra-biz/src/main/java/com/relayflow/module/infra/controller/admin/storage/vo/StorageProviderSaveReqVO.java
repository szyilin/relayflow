package com.relayflow.module.infra.controller.admin.storage.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorageProviderSaveReqVO {

    @NotBlank
    private String provider;

    @NotBlank
    private String endpoint;

    @NotBlank
    private String bucket;

    @NotBlank
    private String accessKey;

    /**
     * Blank on update keeps the existing encrypted secret.
     */
    private String secretKey;

    private Boolean useSsl;

    private String pathPrefix;

    private Boolean isDefault;
}
