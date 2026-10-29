package com.relayflow.module.system.dal.mapper;

import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface SysTenantUserExtMapper {

    /** Repeated checks in one business transaction must not reuse MyBatis session snapshots. */
    @Select("""
            SELECT status, auth_version FROM sys_tenant_user
            WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND deleted = 0
            """)
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    SysTenantUserDO selectIdentity(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Update("""
            <script>
            UPDATE sys_tenant_user
            SET status = #{status},
                auth_version = auth_version + CASE WHEN status = 'ACTIVE' AND #{status} != 'ACTIVE' THEN 1 ELSE 0 END,
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND deleted = 0
              AND status != #{status}
            <if test="expected != null">AND status = #{expected}</if>
            </script>
            """)
    int updateStatus(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                     @Param("status") TenantUserStatus status, @Param("expected") TenantUserStatus expected);

    @Update("""
            <script>
            UPDATE sys_tenant_user
            <set>
                <if test="changes.containsKey('nickname')">nickname = #{changes.nickname},</if>
                <if test="changes.containsKey('avatar')">avatar = #{changes.avatar},</if>
                <if test="changes.containsKey('signature')">signature = #{changes.signature},</if>
                <if test="changes.containsKey('coverFileId')">cover_file_id = #{changes.coverFileId},</if>
                update_time = CURRENT_TIMESTAMP
            </set>
            WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND deleted = 0
            </script>
            """)
    int updateProfile(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                      @Param("changes") Map<String, Object> changes);
}
