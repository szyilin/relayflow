package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Viewer-scoped remark on another tenant member.
 */
@Getter
@Setter
@ToString
@TableName("sys_contact_remark")
public class SysContactRemarkDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("target_user_id")
    private Long targetUserId;

    @TableField("remark_name")
    private String remarkName;

    @TableField("description")
    private String description;
}
