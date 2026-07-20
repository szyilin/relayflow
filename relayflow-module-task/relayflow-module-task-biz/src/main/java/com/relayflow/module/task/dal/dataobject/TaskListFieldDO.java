package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * List-scoped custom field. Table from Flyway V0.1.0.33.
 */
@Getter
@Setter
@ToString
@TableName("task_list_field")
public class TaskListFieldDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("list_id")
    private Long listId;

    private String name;

    @TableField("field_key")
    private String fieldKey;

    @TableField("field_type")
    private String fieldType;

    private Integer rank;
}
