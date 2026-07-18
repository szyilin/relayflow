package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Personal mine group (我负责的自定义分组). Table from Flyway V0.1.0.30.
 */
@Getter
@Setter
@ToString
@TableName("task_mine_group")
public class TaskMineGroupDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    private String name;

    private Integer rank;

    /** 1 = default group */
    @TableField("is_default")
    private Integer isDefault;
}
