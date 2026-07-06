package com.relayflow.module.system.convert;

import com.relayflow.module.system.controller.admin.role.vo.RoleRespVO;
import com.relayflow.module.system.dal.dataobject.SysRoleDO;

public final class RoleConvert {

    private RoleConvert() {
    }

    public static RoleRespVO toVo(SysRoleDO role) {
        RoleRespVO vo = new RoleRespVO();
        vo.setId(role.getId());
        vo.setParentId(role.getParentId());
        vo.setName(role.getName());
        vo.setCode(role.getCode());
        vo.setRoleType(role.getRoleType());
        vo.setDataScope(role.getDataScope());
        vo.setCanDelegate(role.getCanDelegate());
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
