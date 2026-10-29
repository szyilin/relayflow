package com.relayflow.module.infra.dal.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import org.apache.ibatis.annotations.*;
import java.time.OffsetDateTime;
import java.util.List;
@Mapper
@InterceptorIgnore(tenantLine="true")
public interface StorageHistoryExtMapper {
    @Select("""
        SELECT * FROM infra_file
        WHERE tenant_id=#{tenant} AND storage_config_revision_id IS NULL AND provider='minio'
          AND LEFT(storage_uri,LENGTH(#{uriPrefix}))=#{uriPrefix}
          AND create_time>=#{from} AND create_time<=#{to} AND id>#{after}
        ORDER BY id LIMIT #{limit}
        """)
    List<InfraFileDO> page(@Param("tenant") Long tenant,@Param("uriPrefix") String uriPrefix,
                          @Param("from") OffsetDateTime from,@Param("to") OffsetDateTime to,
                          @Param("after") long after,@Param("limit") int limit);
}
