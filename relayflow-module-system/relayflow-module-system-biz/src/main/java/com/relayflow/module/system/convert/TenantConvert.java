package com.relayflow.module.system.convert;

import com.relayflow.module.system.api.tenant.dto.TenantRespDTO;
import com.relayflow.module.system.controller.admin.tenant.vo.TenantRespVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TenantConvert {

    TenantConvert INSTANCE = Mappers.getMapper(TenantConvert.class);

    TenantRespDTO toDto(SysTenantDO tenant);

    TenantRespVO toVo(SysTenantDO tenant);
}
