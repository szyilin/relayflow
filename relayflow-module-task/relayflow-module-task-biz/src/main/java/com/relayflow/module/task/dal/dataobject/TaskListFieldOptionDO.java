package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Option for list custom single-select. Table from Flyway V0.1.0.33.
 */
@Getter
@Setter
@ToString
@TableName("task_list_field_option")
public class TaskListFieldOptionDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("field_id")
    private Long fieldId;

    @TableField("value_key")
    private String valueKey;

    private String label;

    private Integer rank;

    private String color;
}
