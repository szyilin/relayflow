package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.OffsetDateTime;
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
@TableName("task_item")
public class TaskItemDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("title")
    private String title;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("due_time")
    private OffsetDateTime dueTime;

    @TableField("status")
    private String status;
}
