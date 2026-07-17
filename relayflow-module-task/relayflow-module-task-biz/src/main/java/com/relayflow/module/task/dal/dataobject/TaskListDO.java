package com.relayflow.module.task.dal.dataobject;

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
 * @since 2026-07-17
 */
@Getter
@Setter
@ToString
@TableName("task_list")
public class TaskListDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("archived")
    private Integer archived;
}
