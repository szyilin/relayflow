package com.relayflow.module.system.convert;

import com.relayflow.module.system.controller.admin.dept.vo.DeptRespVO;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;

public final class DeptConvert {

    private DeptConvert() {
    }

    public static DeptRespVO toVo(SysDeptDO dept) {
        DeptRespVO vo = new DeptRespVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setName(dept.getName());
        vo.setSort(dept.getSort());
        vo.setLeaderUserId(dept.getLeaderUserId());
        vo.setStatus(dept.getStatus());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }
}
