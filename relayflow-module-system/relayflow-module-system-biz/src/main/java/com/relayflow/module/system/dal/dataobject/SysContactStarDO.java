package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
/**
 * <p>
 * Viewer-scoped starred contacts
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-27
 */
@Getter
@Setter
@ToString
@TableName("sys_contact_star")
public class SysContactStarDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("target_user_id")
    private Long targetUserId;
}
