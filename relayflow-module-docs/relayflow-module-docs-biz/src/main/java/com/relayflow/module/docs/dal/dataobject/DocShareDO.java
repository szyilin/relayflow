package com.relayflow.module.docs.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("doc_share")
public class DocShareDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("object_id")
    private Long objectId;

    @TableField("grantee_user_id")
    private Long granteeUserId;

    /** VIEW | EDIT */
    @TableField("permission")
    private String permission;

    @TableField("granted_by_user_id")
    private Long grantedByUserId;
}
