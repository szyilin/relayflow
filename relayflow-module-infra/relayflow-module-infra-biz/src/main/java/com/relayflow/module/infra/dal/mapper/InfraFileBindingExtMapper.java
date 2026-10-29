package com.relayflow.module.infra.dal.mapper;
import com.relayflow.module.infra.dal.dataobject.InfraFileBindingDO;
import org.apache.ibatis.annotations.*;
@Mapper
public interface InfraFileBindingExtMapper {
    @Insert("""
        INSERT INTO infra_file_binding (id,tenant_id,file_id,biz_type,biz_id)
        VALUES (#{id},#{tenantId},#{fileId},#{bizType},#{bizId})
        ON CONFLICT (tenant_id,biz_type,biz_id,file_id) WHERE deleted=0 DO NOTHING
        """)
    int bind(InfraFileBindingDO binding);
}
