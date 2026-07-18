package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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

    /** User who last assigned this task to someone else; null if never or self-assigned. */
    @TableField(value = "assigner_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long assignerId;

    @TableField(value = "due_time", updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime dueTime;

    @TableField(value = "start_time", updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime startTime;

    @TableField(value = "description", updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    @TableField(value = "remind_before_minutes", updateStrategy = FieldStrategy.ALWAYS)
    private Integer remindBeforeMinutes;

    @TableField("parent_id")
    private Long parentId;

    @TableField("list_id")
    private Long listId;

    @TableField("board_rank")
    private Integer boardRank;

    @TableField("status")
    private String status;
}
