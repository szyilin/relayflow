package com.relayflow.module.docs.service.drive;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.infra.api.file.*;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DocFileAccessPolicy implements FileAccessPolicy, FileHistorySource {
    private final DocObjectMapper objects;
    private final FileApi files;
    public String bizType() { return "doc_object"; }
    public boolean canRead(Long tenantId, Long userId, Long fileId, Long bizId) {
        var row = objects.selectById(bizId);
        return valid(row, tenantId, fileId) && Objects.equals(userId, row.getOwnerUserId());
    }
    private boolean valid(DocObjectDO row, Long tenantId, Long fileId) {
        return row != null && Objects.equals(row.getTenantId(), tenantId) && "FILE".equals(row.getType())
                && "ACTIVE".equals(row.getLifecycle()) && Objects.equals(row.getStorageFileId(), fileId);
    }
    public Page scan(Long tenantId, long afterId, int limit) {
        var rows = objects.selectList(Wrappers.<DocObjectDO>lambdaQuery().eq(DocObjectDO::getTenantId, tenantId)
                .eq(DocObjectDO::getType, "FILE").eq(DocObjectDO::getLifecycle, "ACTIVE")
                .gt(DocObjectDO::getId, afterId).orderByAsc(DocObjectDO::getId).last("LIMIT " + Math.clamp(limit, 1, 500)));
        return new Page(rows.isEmpty() ? afterId : rows.getLast().getId(), rows.size() < limit,
                rows.stream().map(r -> new Reference(r.getId(), r.getStorageFileId(), r.getCreator())).toList());
    }
    public boolean trusted(Long tenantId, Reference ref) {
        var row = objects.selectById(ref.bizId());
        var file = files.getFile(ref.fileId());
        return valid(row, tenantId, ref.fileId()) && Objects.equals(file.getTenantId(), tenantId)
                && file.getCreator() != null && Objects.equals(file.getCreator(), row.getCreator());
    }
}
