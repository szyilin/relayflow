package com.relayflow.module.infra.controller.admin.storage.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorageProviderSaveReqVO {

    @NotBlank
    private String provider;

    private String endpoint;

    private String bucket;

    @NotBlank
    private String accessKey;

    /**
     * Blank on update keeps the existing encrypted secret.
     */
    @lombok.ToString.Exclude
    private String secretKey;

    private String operation;
    private Long revisionId;
    private String historySource;
    private String region;
    private boolean createNewLocation;
    private boolean legacyRelativeKeys;

    private Boolean useSsl;

    private String pathPrefix;

    private Boolean isDefault;
}
