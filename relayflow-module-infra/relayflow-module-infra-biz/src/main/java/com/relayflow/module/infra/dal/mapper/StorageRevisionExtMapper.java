package com.relayflow.module.infra.dal.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.relayflow.module.infra.dal.dataobject.InfraStorageConfigRevisionDO;
import org.apache.ibatis.annotations.*;
import java.time.OffsetDateTime;
import java.util.List;
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface StorageRevisionExtMapper {
    @Select("SELECT * FROM infra_storage_config_revision WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    InfraStorageConfigRevisionDO find(@Param("tenant") Long tenant, @Param("id") Long id);
    @Select("SELECT * FROM infra_storage_config_revision WHERE tenant_id=#{tenant} AND location_key=#{key} AND deleted=0")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    InfraStorageConfigRevisionDO findKey(@Param("tenant") Long tenant, @Param("key") String key);
    @Select("SELECT * FROM infra_storage_config_revision WHERE tenant_id=#{tenant} AND deleted=0 ORDER BY id")
    List<InfraStorageConfigRevisionDO> list(@Param("tenant") Long tenant);
    @Insert("""
        INSERT INTO infra_storage_config_revision(id,tenant_id,provider,source,location_key,config_json)
        VALUES(#{id},#{tenantId},#{provider},#{source},#{locationKey},#{configJson})
        ON CONFLICT (tenant_id,location_key) DO NOTHING
        """)
    int insertIfMissing(InfraStorageConfigRevisionDO row);
    @Update("""
        UPDATE infra_storage_config_revision
        SET config_json=(config_json::jsonb || jsonb_build_object('access_key',#{accessKey},'secret_key_enc',#{secret}))::text,
            update_time=CURRENT_TIMESTAMP
        WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0
        """)
    int credentials(@Param("tenant") Long tenant, @Param("id") Long id,
                    @Param("accessKey") String accessKey, @Param("secret") String secret);
    @Select("SELECT count(*) FROM infra_file WHERE tenant_id=#{tenant} AND storage_config_revision_id=#{id}")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    long files(@Param("tenant") Long tenant, @Param("id") Long id);
    @Select("SELECT count(*) FROM infra_file_upload_session WHERE tenant_id=#{tenant} AND storage_config_revision_id=#{id} AND status='pending' AND expires_at>CURRENT_TIMESTAMP AND deleted=0")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    long sessions(@Param("tenant") Long tenant, @Param("id") Long id);
    @Select("SELECT count(*) FROM infra_file WHERE tenant_id=#{tenant} AND storage_config_revision_id IS NULL")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    long unresolvedFiles(@Param("tenant") Long tenant);
    @Select("SELECT count(*) FROM infra_file_upload_session WHERE tenant_id=#{tenant} AND storage_config_revision_id IS NULL AND status='pending' AND expires_at>CURRENT_TIMESTAMP AND deleted=0")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    long unresolvedSessions(@Param("tenant") Long tenant);
    @Update("UPDATE infra_storage_provider SET config_json=#{json}, storage_config_revision_id=#{revision}, update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0")
    int saveDraft(@Param("tenant") Long tenant, @Param("id") Long id, @Param("json") String json, @Param("revision") Long revision);
    @Update("UPDATE infra_storage_provider SET active_storage_config_revision_id=#{revision}, is_default=1, update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0")
    int activate(@Param("tenant") Long tenant, @Param("id") Long id, @Param("revision") Long revision);
    @Update("UPDATE infra_file SET storage_config_revision_id=#{revision} WHERE tenant_id=#{tenant} AND id=#{id} AND storage_config_revision_id IS NULL")
    int bindFile(@Param("tenant") Long tenant, @Param("id") Long id, @Param("revision") Long revision);
}
