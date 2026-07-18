package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * List-local group. Table from Flyway V0.1.0.32.
 */
@Getter
@Setter
@ToString
@TableName("task_list_group")
public class TaskListGroupDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("list_id")
    private Long listId;

    private String name;

    private Integer rank;

    /** 1 = default group */
    @TableField("is_default")
    private Integer isDefault;
}
