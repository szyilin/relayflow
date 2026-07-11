package com.relayflow.module.infra.service.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StorageProviderConfigJson {

    private String endpoint;

    private String bucket;

    @JsonProperty("access_key")
    private String accessKey;

    @JsonProperty("secret_key_enc")
    private String secretKeyEnc;

    @JsonProperty("use_ssl")
    private Boolean useSsl;

    @JsonProperty("path_prefix")
    private String pathPrefix;
}
