package com.relayflow.module.agent.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * Configurable Agent definition; model_ref -> agent_llm_connection.code
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Getter
@Setter
@ToString
@TableName(value = "agent_def", autoResultMap = true)
public class AgentDefDO extends TenantBaseDO {


    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * References agent_llm_connection.code within tenant
     */
    @TableField("model_ref")
    private String modelRef;

    /**
     * JSON array of skill codes; empty until skill catalog
     */
    @TableField(value = "skill_ids_json", typeHandler = JsonbTypeHandler.class)
    private String skillIdsJson;

    @TableField("carrier_bot_id")
    private Long carrierBotId;

    @TableField("status")
    private String status;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("visibility")
    private String visibility;
}
