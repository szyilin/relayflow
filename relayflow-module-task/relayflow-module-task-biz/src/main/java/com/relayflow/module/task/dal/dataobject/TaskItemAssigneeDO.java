package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Task multi-assignees (负责人集合).
 * Shape aligned with task_follower; table from Flyway V0.1.0.29.
 */
@Getter
@Setter
@ToString
@TableName("task_item_assignee")
public class TaskItemAssigneeDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("task_id")
    private Long taskId;

    @TableField("user_id")
    private Long userId;
}
