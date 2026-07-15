package com.relayflow.module.system.convert;

import com.relayflow.module.system.controller.admin.user.vo.UserGetRespVO;
import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.enums.TenantUserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    @Mapping(target = "dept", source = "deptName")
    @Mapping(target = "memberStatus", expression = "java(memberStatus.name())")
    @Mapping(target = "status", expression = "java(toStatusCode(memberStatus))")
    UserRespVO toVo(SysUserDO user, TenantUserStatus memberStatus, String deptName);

    @Mapping(target = "memberStatus", expression = "java(memberStatus.name())")
    @Mapping(target = "status", expression = "java(toStatusCode(memberStatus))")
    UserGetRespVO toGetVo(SysUserDO user, TenantUserStatus memberStatus, Long deptId, List<Long> roleIds);

    default int toStatusCode(TenantUserStatus memberStatus) {
        return memberStatus == TenantUserStatus.ACTIVE ? 0 : 1;
    }

    default TenantUserStatus toMemberStatus(Integer statusCode) {
        return statusCode != null && statusCode == 0
                ? TenantUserStatus.ACTIVE
                : TenantUserStatus.SUSPENDED;
    }
}
