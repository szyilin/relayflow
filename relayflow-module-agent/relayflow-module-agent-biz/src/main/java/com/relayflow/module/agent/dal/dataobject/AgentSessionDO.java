package com.relayflow.module.agent.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * Agent workspace chat session (not IM conversation)
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Getter
@Setter
@ToString
@TableName("agent_session")
public class AgentSessionDO extends TenantBaseDO {


    @TableField("agent_id")
    private Long agentId;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    @TableField("status")
    private String status;
}
