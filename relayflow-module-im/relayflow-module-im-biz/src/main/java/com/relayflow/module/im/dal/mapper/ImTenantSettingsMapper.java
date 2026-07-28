package com.relayflow.module.im.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.im.dal.dataobject.ImTenantSettingsDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImTenantSettingsMapper extends BaseMapper<ImTenantSettingsDO> {
}
