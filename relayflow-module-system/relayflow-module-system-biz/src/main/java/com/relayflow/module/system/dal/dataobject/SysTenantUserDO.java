package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
/**
 * <p>
 * 
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-15
 */
@Getter
@Setter
@ToString
@TableName("sys_tenant_user")
public class SysTenantUserDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField("status")
    private TenantUserStatus status;

    /** Display nickname within this tenant; null falls back to sys_user. */
    @TableField("nickname")
    private String nickname;

    /** Avatar fileId within this tenant. */
    @TableField("avatar")
    private String avatar;

    @TableField("signature")
    private String signature;

    @TableField("cover_file_id")
    private String coverFileId;
}
