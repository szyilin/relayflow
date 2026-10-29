package com.relayflow.module.infra.api.file;

import java.util.List;

/** Trusted resource enumeration for operator-triggered, tenant-scoped history repair. */
public interface FileHistorySource {
    String bizType();
    Page scan(Long tenantId, long afterId, int limit);
    boolean trusted(Long tenantId, Reference reference);
    record Page(long nextId, boolean done, List<Reference> references) {}
    record Reference(Long bizId, Long fileId, Long actorId) {}
}
