package com.relayflow.module.infra.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
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
@TableName("infra_storage_provider")
public class InfraStorageProviderDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("provider")
    private String provider;

    @TableField("status")
    private String status;

    @TableField("is_default")
    private Integer isDefault;

    @TableField("config_json")
    private String configJson;
}
