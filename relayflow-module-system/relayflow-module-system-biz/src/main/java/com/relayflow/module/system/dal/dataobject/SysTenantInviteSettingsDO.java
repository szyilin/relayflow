package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Per-tenant org invite mode and non-admin constraints.
 *
 * @author relayflow-codegen
 * @since 2026-07-27
 */
@Getter
@Setter
@ToString
@TableName("sys_tenant_invite_settings")
public class SysTenantInviteSettingsDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    /** OFF | ADMIN_ONLY | ALL_MEMBERS */
    @TableField("invite_mode")
    private String inviteMode;

    /** INVITER_DEPT | DESIGNATED_DEPTS */
    @TableField("non_admin_scope")
    private String nonAdminScope;

    /** JSON array of dept id strings */
    @TableField(value = "non_admin_dept_ids", typeHandler = JsonbTypeHandler.class)
    private String nonAdminDeptIds;

    /** Invite TTL days for non-admin inviters */
    @TableField("non_admin_expire_days")
    private Integer nonAdminExpireDays;

    /** Whether non-admin invites need approval */
    @TableField("non_admin_require_approval")
    private Boolean nonAdminRequireApproval;
}
