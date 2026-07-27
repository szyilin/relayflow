package com.relayflow.module.docs.service.visit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.dataobject.DocUserVisitDO;
import com.relayflow.module.docs.dal.mapper.DocUserVisitMapper;
import com.relayflow.module.docs.enums.DocConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DocVisitService {

    private final DocUserVisitMapper docUserVisitMapper;

    @Transactional(rollbackFor = Exception.class)
    public void upsertVisit(Long tenantId, Long userId, DocObjectDO object, boolean ownedByMe) {
        if (object == null || object.getId() == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        DocUserVisitDO existing = docUserVisitMapper.selectOne(
                Wrappers.<DocUserVisitDO>lambdaQuery()
                        .eq(DocUserVisitDO::getUserId, userId)
                        .eq(DocUserVisitDO::getObjectId, object.getId())
                        .last("LIMIT 1"));
        String source = ownedByMe ? DocConstants.VISIT_SOURCE_OWNER : DocConstants.VISIT_SOURCE_SHARED;
        String title = object.getTitle() != null ? object.getTitle() : "";
        if (existing != null) {
            existing.setLastOpenedAt(now);
            existing.setTitleSnapshot(title);
            existing.setSource(source);
            existing.setUpdater(userId);
            existing.setUpdateTime(now);
            docUserVisitMapper.updateById(existing);
            return;
        }
        DocUserVisitDO row = new DocUserVisitDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setObjectId(object.getId());
        row.setLastOpenedAt(now);
        row.setTitleSnapshot(title);
        row.setSource(source);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        docUserVisitMapper.insert(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeVisit(Long userId, Long objectId) {
        DocUserVisitDO existing = docUserVisitMapper.selectOne(
                Wrappers.<DocUserVisitDO>lambdaQuery()
                        .eq(DocUserVisitDO::getUserId, userId)
                        .eq(DocUserVisitDO::getObjectId, objectId)
                        .last("LIMIT 1"));
        if (existing != null) {
            docUserVisitMapper.deleteById(existing.getId());
        }
    }
}
