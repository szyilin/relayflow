package com.relayflow.module.agent.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * Tenant LLM provider connection; code is Agent modelRef
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Getter
@Setter
@ToString
@TableName("agent_llm_connection")
public class AgentLlmConnectionDO extends TenantBaseDO {


    /**
     * Unique modelRef within tenant
     */
    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("provider_type")
    private String providerType;

    @TableField("base_url")
    private String baseUrl;

    /**
     * AES-GCM encrypted API key
     */
    @TableField("api_key_enc")
    private String apiKeyEnc;

    @TableField("model_name")
    private String modelName;

    @TableField("enabled")
    private Integer enabled;
}
