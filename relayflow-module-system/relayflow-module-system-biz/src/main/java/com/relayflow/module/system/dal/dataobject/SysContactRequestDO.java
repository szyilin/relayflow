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
 * Add-contact requests addressed to a tenant member
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-27
 */
@Getter
@Setter
@ToString
@TableName("sys_contact_request")
public class SysContactRequestDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("from_user_id")
    private Long fromUserId;

    @TableField("to_user_id")
    private Long toUserId;

    /**
     * pending | accepted | rejected
     */
    @TableField("status")
    private String status;

    @TableField("subtitle")
    private String subtitle;
}
