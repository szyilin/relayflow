package com.relayflow.module.docs.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.docs.dal.dataobject.DocTenantSettingsDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocTenantSettingsMapper extends BaseMapper<DocTenantSettingsDO> {
}
