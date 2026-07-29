package com.relayflow.module.calendar.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.calendar.dal.dataobject.CalTenantSettingsDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CalTenantSettingsMapper extends BaseMapper<CalTenantSettingsDO> {
}
