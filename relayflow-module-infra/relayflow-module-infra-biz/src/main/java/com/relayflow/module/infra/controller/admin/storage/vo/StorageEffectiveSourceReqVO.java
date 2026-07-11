package com.relayflow.module.infra.controller.admin.storage.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorageEffectiveSourceReqVO {

    /**
     * {@code bootstrap} or {@code tenant}.
     */
    @NotBlank
    private String source;
}
