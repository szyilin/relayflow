package com.relayflow.module.infra.controller.admin.storage.vo;

import lombok.Data;

/**
 * Non-sensitive bootstrap storage summary for admin UI (no endpoint / credentials).
 */
@Data
public class StorageBootstrapSummaryVO {

    private boolean available;

    private String provider;

    private boolean credentialsConfigured;
}
