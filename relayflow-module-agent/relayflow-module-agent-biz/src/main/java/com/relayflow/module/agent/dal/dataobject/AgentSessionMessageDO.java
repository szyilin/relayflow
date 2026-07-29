package com.relayflow.module.agent.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * Agent session turns; never written to im_message by this path
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Getter
@Setter
@ToString
@TableName("agent_session_message")
public class AgentSessionMessageDO extends TenantBaseDO {


    @TableField("session_id")
    private Long sessionId;

    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    /** JSON array of parts; null for legacy rows. */
    @TableField("parts_json")
    private String partsJson;

    @TableField("seq")
    private Integer seq;
}
