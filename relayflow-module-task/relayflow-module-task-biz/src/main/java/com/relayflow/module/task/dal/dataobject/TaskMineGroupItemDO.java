package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Task membership in a personal mine group. Table from Flyway V0.1.0.30.
 */
@Getter
@Setter
@ToString
@TableName("task_mine_group_item")
public class TaskMineGroupItemDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField("task_id")
    private Long taskId;

    @TableField("group_id")
    private Long groupId;

    private Integer rank;
}
