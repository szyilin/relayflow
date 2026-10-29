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

    @org.apache.ibatis.annotations.Update("""
        UPDATE doc_object SET last_opened_at=#{now}, updater=#{userId}, update_time=#{now}
        WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 AND lifecycle='ACTIVE'
        """)
    int touchOpened(@Param("tenantId") Long tenantId, @Param("id") Long id,
                    @Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @org.apache.ibatis.annotations.Update("""
        UPDATE doc_object SET title=#{title}, updater=#{userId}, update_time=#{now}
        WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 AND lifecycle='ACTIVE'
        """)
    int rename(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("title") String title,
               @Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @org.apache.ibatis.annotations.Update("""
        UPDATE doc_object SET body=CAST(#{body} AS jsonb), content_version=content_version+1,
                              updater=#{userId}, update_time=#{now}
        WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 AND lifecycle='ACTIVE'
          AND type='RICH_DOC' AND content_version=#{version}
        """)
    int saveBody(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("version") int version,
                 @Param("body") String body, @Param("userId") Long userId, @Param("now") OffsetDateTime now);


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
