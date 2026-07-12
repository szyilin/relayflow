package com.relayflow.module.system.convert;

import com.relayflow.module.system.controller.admin.user.vo.UserGetRespVO;
import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class UserConvert {

    public UserRespVO toVo(SysUserDO user, TenantUserStatus memberStatus, String deptName) {
        UserRespVO vo = new UserRespVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setMobile(user.getMobile());
        vo.setDept(deptName);
        vo.setMemberStatus(memberStatus.name());
        vo.setStatus(toStatusCode(memberStatus));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    public UserGetRespVO toGetVo(SysUserDO user, TenantUserStatus memberStatus,
                                 Long deptId, List<Long> roleIds) {
        UserGetRespVO vo = new UserGetRespVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setMobile(user.getMobile());
        vo.setEmail(user.getEmail());
        vo.setMemberStatus(memberStatus.name());
        vo.setStatus(toStatusCode(memberStatus));
        vo.setDeptId(deptId);
        vo.setRoleIds(roleIds);
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private int toStatusCode(TenantUserStatus memberStatus) {
        return memberStatus == TenantUserStatus.ACTIVE ? 0 : 1;
    }

    public TenantUserStatus toMemberStatus(Integer statusCode) {
        return statusCode != null && statusCode == 0
                ? TenantUserStatus.ACTIVE
                : TenantUserStatus.SUSPENDED;
    }
}
