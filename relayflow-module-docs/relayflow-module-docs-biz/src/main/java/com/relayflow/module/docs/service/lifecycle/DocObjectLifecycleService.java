package com.relayflow.module.docs.service.lifecycle;

import com.relayflow.module.docs.dal.mapper.DocObjectExtMapper;
import com.relayflow.module.docs.enums.DocConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Keeps {@code lifecycle}/{@code deleted_at} in sync with workspace soft-delete ({@code @TableLogic}).
 */
@Service
@RequiredArgsConstructor
public class DocObjectLifecycleService {

    private final DocObjectExtMapper docObjectExtMapper;

    public void markDeleted(
            Long tenantId, Long objectId, Long ownerUserId, Long updater, OffsetDateTime deletedAt) {
        if (objectId == null) {
            return;
        }
        docObjectExtMapper.updateLifecycleAndOwner(
                tenantId,
                objectId,
                DocConstants.LIFECYCLE_DELETED,
                1,
                deletedAt,
                ownerUserId,
                updater,
                deletedAt);
    }
}
