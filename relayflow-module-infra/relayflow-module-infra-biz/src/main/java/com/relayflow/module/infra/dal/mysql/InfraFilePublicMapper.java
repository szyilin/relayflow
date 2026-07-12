package com.relayflow.module.infra.dal.mysql;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Cross-tenant lookup for {@code access_level=public} files served via permitAll download.
 */
@Mapper
public interface InfraFilePublicMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM infra_file WHERE id = #{fileId} AND deleted = 0 LIMIT 1")
    InfraFileDO selectByIdGlobal(@Param("fileId") Long fileId);
}
