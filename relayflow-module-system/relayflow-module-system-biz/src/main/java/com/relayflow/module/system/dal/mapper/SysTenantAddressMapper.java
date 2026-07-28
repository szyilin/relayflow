package com.relayflow.module.system.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.system.dal.dataobject.SysTenantAddressDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Per-tenant enterprise addresses (job/interview/store types) Mapper 接口
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Mapper
public interface SysTenantAddressMapper extends BaseMapper<SysTenantAddressDO> {

}

