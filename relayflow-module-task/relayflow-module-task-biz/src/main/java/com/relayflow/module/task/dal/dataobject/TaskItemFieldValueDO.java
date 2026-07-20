package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * EAV value for task custom field. Table from Flyway V0.1.0.33.
 */
@Getter
@Setter
@ToString
@TableName("task_item_field_value")
public class TaskItemFieldValueDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("item_id")
    private Long itemId;

    @TableField("field_id")
    private Long fieldId;

    @TableField("option_id")
    private Long optionId;
}
