package com.relayflow.module.docs.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.docs.dal.dataobject.DocStorageQuotaDeptRuleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DocStorageQuotaDeptRuleMapper extends BaseMapper<DocStorageQuotaDeptRuleDO> {

    /** Physical delete so UNIQUE(tenant_id, dept_id) can be reused. */
    @Delete("DELETE FROM doc_storage_quota_dept_rule WHERE id = #{id} AND tenant_id = #{tenantId}")
    int physicalDeleteById(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
