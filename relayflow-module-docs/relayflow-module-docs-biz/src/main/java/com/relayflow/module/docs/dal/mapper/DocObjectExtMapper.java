package com.relayflow.module.docs.dal.mapper;

import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.dataobject.DocOwnerStorageUsageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Admin file-ops queries that include soft-deleted rows (bypass {@code @TableLogic}).
 * SELECT never includes {@code body}.
 */
@Mapper
public interface DocObjectExtMapper {

    List<DocObjectDO> selectAdminPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("lifecycle") String lifecycle,
            @Param("ownerUserId") Long ownerUserId,
            @Param("offset") long offset,
            @Param("limit") long limit);

    long countAdminPage(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("lifecycle") String lifecycle,
            @Param("ownerUserId") Long ownerUserId);

    List<DocObjectDO> selectByIdsIncludeDeleted(
            @Param("tenantId") Long tenantId,
            @Param("ids") List<Long> ids);

    int updateLifecycleAndOwner(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("lifecycle") String lifecycle,
            @Param("deleted") int deleted,
            @Param("deletedAt") OffsetDateTime deletedAt,
            @Param("ownerUserId") Long ownerUserId,
            @Param("updater") Long updater,
            @Param("updateTime") OffsetDateTime updateTime);

    int purgeObject(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("updater") Long updater,
            @Param("updateTime") OffsetDateTime updateTime);

    /**
     * Sum ACTIVE FILE sizes ({@code doc_object.size_bytes}) for owners.
     */
    List<DocOwnerStorageUsageRow> sumActiveFileUsageByOwners(
            @Param("tenantId") Long tenantId,
            @Param("ownerUserIds") List<Long> ownerUserIds);

    Long sumActiveFileUsageForOwner(
            @Param("tenantId") Long tenantId,
            @Param("ownerUserId") Long ownerUserId);

    List<DocObjectDO> selectFilesNeedingSizeBackfill(
            @Param("tenantId") Long tenantId,
            @Param("afterId") Long afterId,
            @Param("limit") int limit);

    int updateSizeBytes(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("sizeBytes") Long sizeBytes,
            @Param("updater") Long updater,
            @Param("updateTime") OffsetDateTime updateTime);
}
