package com.relayflow.module.agent.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@TableName("agent_automation")
public class AgentAutomationDO extends TenantBaseDO {

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("agent_id")
    private Long agentId;

    @TableField("title")
    private String title;

    @TableField("prompt")
    private String prompt;

    @TableField("cron_expr")
    private String cronExpr;

    @TableField("status")
    private String status;

    @TableField("target_user_id")
    private Long targetUserId;

    @TableField("next_run_at")
    private OffsetDateTime nextRunAt;

    @TableField("last_run_at")
    private OffsetDateTime lastRunAt;

    @TableField("last_status")
    private String lastStatus;

    @TableField("last_error")
    private String lastError;
}
