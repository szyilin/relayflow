package com.relayflow.module.system.convert;

import com.relayflow.module.system.controller.admin.role.vo.RoleRespVO;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RoleConvert {

    RoleConvert INSTANCE = Mappers.getMapper(RoleConvert.class);

    RoleRespVO toVo(SysRoleDO role);
}
