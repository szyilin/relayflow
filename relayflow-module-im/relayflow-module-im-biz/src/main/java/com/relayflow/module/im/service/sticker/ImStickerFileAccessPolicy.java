package com.relayflow.module.im.service.sticker;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.infra.api.file.*;
import com.relayflow.module.im.dal.dataobject.ImUserStickerDO;
import com.relayflow.module.im.dal.mapper.ImUserStickerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ImStickerFileAccessPolicy implements FileAccessPolicy, FileHistorySource {
    private final ImUserStickerMapper stickers;
    private final FileApi files;
    public String bizType() { return "im_user_sticker"; }
    public boolean canRead(Long tenantId, Long userId, Long fileId, Long bizId) {
        var row = stickers.selectById(bizId);
        return row != null && Objects.equals(row.getTenantId(), tenantId) && Objects.equals(row.getUserId(), userId)
                && Objects.equals(row.getFileId(), fileId);
    }
    public Page scan(Long tenantId, long afterId, int limit) {
        var rows = stickers.selectList(Wrappers.<ImUserStickerDO>lambdaQuery().eq(ImUserStickerDO::getTenantId, tenantId)
                .gt(ImUserStickerDO::getId, afterId).orderByAsc(ImUserStickerDO::getId).last("LIMIT " + Math.clamp(limit, 1, 500)));
        return new Page(rows.isEmpty() ? afterId : rows.getLast().getId(), rows.size() < limit,
                rows.stream().map(r -> new Reference(r.getId(), r.getFileId(), r.getUserId())).toList());
    }
    public boolean trusted(Long tenantId, Reference ref) {
        var file = files.getFile(ref.fileId());
        return Objects.equals(file.getTenantId(), tenantId) && file.getCreator() != null && Objects.equals(file.getCreator(), ref.actorId());
    }
}
