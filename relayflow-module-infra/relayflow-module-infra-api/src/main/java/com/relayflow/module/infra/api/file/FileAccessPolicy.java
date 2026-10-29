package com.relayflow.module.infra.api.file;

/** A current, domain-owned authorization decision; implementations never sign URLs. */
public interface FileAccessPolicy {
    String bizType();
    boolean canRead(Long tenantId, Long userId, Long fileId, Long bizId);
}
