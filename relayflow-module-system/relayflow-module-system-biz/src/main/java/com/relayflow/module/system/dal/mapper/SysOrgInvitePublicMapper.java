package com.relayflow.module.system.dal.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.relayflow.module.system.dal.dataobject.SysOrgInviteDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Cross-tenant lookup for invite consume (token / code are globally unique when ACTIVE).
 */
@Mapper
public interface SysOrgInvitePublicMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM sys_org_invite
            WHERE deleted = 0 AND token = #{token}
            LIMIT 1
            """)
    SysOrgInviteDO selectByTokenGlobal(@Param("token") String token);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM sys_org_invite
            WHERE deleted = 0 AND invite_code = #{inviteCode}
            LIMIT 1
            """)
    SysOrgInviteDO selectByInviteCodeGlobal(@Param("inviteCode") String inviteCode);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM sys_org_invite
            WHERE deleted = 0 AND id = #{id}
            LIMIT 1
            """)
    SysOrgInviteDO selectByIdGlobal(@Param("id") Long id);
}
