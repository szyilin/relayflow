package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
/**
 * <p>
 * Task activity / dynamics feed
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-17
 */
@Getter
@Setter
@ToString
@TableName("task_activity")
public class TaskActivityDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("task_id")
    private Long taskId;

    @TableField("task_title")
    private String taskTitle;

    @TableField("actor_id")
    private Long actorId;

    /**
     * created|field_changed|subtask_*|follower_*|commented|assigned
     */
    @TableField("type")
    private String type;

    /**
     * Human-readable activity summary
     */
    @TableField("summary")
    private String summary;
}
