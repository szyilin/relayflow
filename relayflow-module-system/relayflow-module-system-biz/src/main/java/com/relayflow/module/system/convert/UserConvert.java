package com.relayflow.module.system.convert;

import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserConvert {

    public UserRespVO toVo(SysUserDO user, TenantUserStatus memberStatus, String deptName) {
        UserRespVO vo = new UserRespVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setDept(deptName);
        vo.setStatus(toStatusCode(memberStatus));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private int toStatusCode(TenantUserStatus memberStatus) {
        return memberStatus == TenantUserStatus.ACTIVE ? 0 : 1;
    }
}
