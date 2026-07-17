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
 * Task followers (我关注的)
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-17
 */
@Getter
@Setter
@ToString
@TableName("task_follower")
public class TaskFollowerDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    /**
     * task_item.id
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * Follower user id
     */
    @TableField("user_id")
    private Long userId;
}
