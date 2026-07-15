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
@TableName("sys_menu")
public class SysMenuDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("type")
    private Integer type;

    @TableField("path")
    private String path;

    @TableField("component")
    private String component;

    @TableField("permission_id")
    private Long permissionId;

    @TableField("icon")
    private String icon;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private Integer status;

    @TableField("visible")
    private Integer visible;
}
