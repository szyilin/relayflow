package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
/**
 * <p>
 * Org invite process entity; membership written on consume
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-27
 */
@Getter
@Setter
@ToString
@TableName("sys_org_invite")
public class SysOrgInviteDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    /**
     * SHARED | MOBILE (LINK/QR/CODE share SHARED row)
     */
    @TableField("channel")
    private String channel;

    @TableField("token")
    private String token;

    @TableField("invite_code")
    private String inviteCode;

    @TableField("inviter_user_id")
    private Long inviterUserId;

    @TableField("target_mobile")
    private String targetMobile;

    @TableField("target_user_id")
    private Long targetUserId;

    @TableField("target_dept_id")
    private Long targetDeptId;

    @TableField(value = "target_role_ids", typeHandler = JsonbTypeHandler.class)
    private String targetRoleIds;

    /**
     * ACTIVE | PENDING_APPROVAL | CONSUMED | REVOKED | EXPIRED | REJECTED
     */
    @TableField("status")
    private String status;

    @TableField("expires_at")
    private OffsetDateTime expiresAt;

    @TableField("approved_by")
    private Long approvedBy;

    @TableField("approved_at")
    private OffsetDateTime approvedAt;

    @TableField("consumed_at")
    private OffsetDateTime consumedAt;

    @TableField("membership_user_id")
    private Long membershipUserId;
}
