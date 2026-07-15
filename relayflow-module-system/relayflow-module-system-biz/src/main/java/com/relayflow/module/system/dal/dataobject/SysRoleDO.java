package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import com.relayflow.module.system.enums.DataScope;
import com.relayflow.module.system.enums.RoleType;
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
@TableName("sys_role")
public class SysRoleDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("role_type")
    private RoleType roleType;

    @TableField("data_scope")
    private DataScope dataScope;

    @TableField("can_delegate")
    private Integer canDelegate;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;
}
