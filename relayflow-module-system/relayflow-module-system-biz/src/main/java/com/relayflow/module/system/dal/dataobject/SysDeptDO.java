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
 * 
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-15
 */
@Getter
@Setter
@ToString
@TableName("sys_dept")
public class SysDeptDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("sort")
    private Integer sort;

    @TableField("leader_user_id")
    private Long leaderUserId;

    @TableField("status")
    private Integer status;
}
