package com.relayflow.module.system.convert;

import com.relayflow.module.system.controller.admin.dept.vo.DeptRespVO;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DeptConvert {

    DeptConvert INSTANCE = Mappers.getMapper(DeptConvert.class);

    DeptRespVO toVo(SysDeptDO dept);
}
