package com.relayflow.module.infra.dal.mysql;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cross-tenant notification queries (receiver may hold invites from multiple tenants).
 */
@Mapper
public interface InfraNotifyPublicMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM infra_notify
            WHERE mobile = #{mobile}
              AND user_id IS NULL
              AND read_flag = 0
              AND deleted = 0
            ORDER BY create_time DESC
            """)
    List<InfraNotifyDO> selectUnreadByMobile(@Param("mobile") String mobile);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM infra_notify
            WHERE user_id = #{userId}
              AND deleted = 0
            ORDER BY create_time DESC
            """)
    List<InfraNotifyDO> selectByUserId(@Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT COUNT(*) FROM infra_notify
            WHERE user_id = #{userId}
              AND read_flag = 0
              AND deleted = 0
            """)
    long countUnreadByUserId(@Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT COUNT(*) FROM infra_notify
            WHERE user_id = #{userId}
              AND deleted = 0
            """)
    long countByUserId(@Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM infra_notify
            WHERE user_id = #{userId}
              AND deleted = 0
            ORDER BY create_time DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<InfraNotifyDO> selectPageByUserId(@Param("userId") Long userId,
                                           @Param("pageSize") int pageSize,
                                           @Param("offset") long offset);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM infra_notify
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted = 0
            """)
    InfraNotifyDO selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE infra_notify
            SET read_flag = 1,
                update_time = #{updateTime}
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted = 0
            """)
    int markReadById(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("updateTime") OffsetDateTime updateTime);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE infra_notify
            SET user_id = #{userId},
                update_time = #{updateTime}
            WHERE mobile = #{mobile}
              AND user_id IS NULL
              AND deleted = 0
            """)
    int updateUserIdByMobile(@Param("mobile") String mobile,
                             @Param("userId") Long userId,
                             @Param("updateTime") OffsetDateTime updateTime);
}
