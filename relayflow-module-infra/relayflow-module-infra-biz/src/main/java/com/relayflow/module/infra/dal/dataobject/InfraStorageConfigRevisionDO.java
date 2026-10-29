package com.relayflow.module.infra.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
 * @since 2026-10-29
 */
@Getter
@Setter
@ToString
@TableName("infra_storage_config_revision")
public class InfraStorageConfigRevisionDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField(value = "provider", updateStrategy = FieldStrategy.NEVER)
    private String provider;

    @TableField(value = "source", updateStrategy = FieldStrategy.NEVER)
    private String source;

    @TableField(value = "location_key", updateStrategy = FieldStrategy.NEVER)
    private String locationKey;

    /**
     * Immutable location metadata and AES-GCM encrypted secret; credentials rotate explicitly
     */
    @TableField(value = "config_json", updateStrategy = FieldStrategy.NEVER)
    @ToString.Exclude
    private String configJson;
}
