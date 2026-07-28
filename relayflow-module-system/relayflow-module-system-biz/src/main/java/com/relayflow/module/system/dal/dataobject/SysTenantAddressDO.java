package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
/**
 * <p>
 * Per-tenant enterprise addresses (job/interview/store types)
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Getter
@Setter
@ToString
@TableName(value = "sys_tenant_address", autoResultMap = true)
public class SysTenantAddressDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    /**
     * JSON array of JOB | INTERVIEW | STORE
     */
    @TableField(value = "types", typeHandler = JsonbTypeHandler.class)
    private String types;

    /**
     * infra_geo_region.id
     */
    @TableField("region_id")
    private Long regionId;

    @TableField("region_path")
    private String regionPath;

    @TableField("detail")
    private String detail;

    /**
     * ENABLED | DISABLED
     */
    @TableField("status")
    private String status;
}
