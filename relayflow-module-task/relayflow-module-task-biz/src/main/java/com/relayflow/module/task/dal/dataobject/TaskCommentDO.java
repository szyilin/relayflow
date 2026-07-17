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
 * Task plain-text comments
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-17
 */
@Getter
@Setter
@ToString
@TableName("task_comment")
public class TaskCommentDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("task_id")
    private Long taskId;

    /**
     * Comment author user id
     */
    @TableField("author_id")
    private Long authorId;

    @TableField("content")
    private String content;
}
