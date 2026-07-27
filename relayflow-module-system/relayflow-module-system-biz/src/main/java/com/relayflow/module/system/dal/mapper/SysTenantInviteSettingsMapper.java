package com.relayflow.module.system.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.system.dal.dataobject.SysTenantInviteSettingsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Per-tenant org invite mode and non-admin constraints Mapper.
 *
 * @author relayflow-codegen
 * @since 2026-07-27
 */
@Mapper
public interface SysTenantInviteSettingsMapper extends BaseMapper<SysTenantInviteSettingsDO> {
}
