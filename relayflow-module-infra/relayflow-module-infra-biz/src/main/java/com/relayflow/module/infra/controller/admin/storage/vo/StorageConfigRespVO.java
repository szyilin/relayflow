package com.relayflow.module.infra.controller.admin.storage.vo;

import lombok.Data;

import java.util.List;

@Data
public class StorageConfigRespVO {

    /**
     * {@code bootstrap} or {@code tenant}.
     */
    private String effectiveSource;

    private StorageBootstrapSummaryVO bootstrap;

    private List<StorageProviderRespVO> providers;
}
