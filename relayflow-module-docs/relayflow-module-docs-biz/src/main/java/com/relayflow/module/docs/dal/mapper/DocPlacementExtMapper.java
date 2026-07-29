package com.relayflow.module.docs.dal.mapper;

import com.relayflow.module.docs.dal.dataobject.DocDriveItemDO;
import com.relayflow.module.docs.dal.dataobject.DocLibraryNodeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/**
 * Placement lookups/updates including soft-deleted rows (admin transfer / restore).
 */
@Mapper
public interface DocPlacementExtMapper {

    DocLibraryNodeDO selectLibraryNodeByObjectIdIncludeDeleted(
            @Param("tenantId") Long tenantId,
            @Param("objectId") Long objectId);

    DocDriveItemDO selectDriveItemByObjectIdIncludeDeleted(
            @Param("tenantId") Long tenantId,
            @Param("objectId") Long objectId);

    int updateLibraryNodeOwnerRoot(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("ownerUserId") Long ownerUserId,
            @Param("deleted") int deleted,
            @Param("updater") Long updater,
            @Param("updateTime") OffsetDateTime updateTime);

    int updateDriveItemOwnerRoot(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("ownerUserId") Long ownerUserId,
            @Param("deleted") int deleted,
            @Param("updater") Long updater,
            @Param("updateTime") OffsetDateTime updateTime);

    int softDeleteLibraryNode(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("updater") Long updater,
            @Param("updateTime") OffsetDateTime updateTime);

    int softDeleteDriveItem(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("updater") Long updater,
            @Param("updateTime") OffsetDateTime updateTime);
}
