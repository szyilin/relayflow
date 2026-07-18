package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Task membership in a list. Table from Flyway V0.1.0.31.
 */
@Getter
@Setter
@ToString
@TableName("task_list_item")
public class TaskListItemDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("list_id")
    private Long listId;

    @TableField("task_id")
    private Long taskId;

    @TableField("group_id")
    private Long groupId;

    private Integer rank;
}
