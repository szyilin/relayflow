package com.relayflow.module.infra.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.BaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
/**
 * <p>
 * Platform China admin-division tree; shared across tenants
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Getter
@Setter
@ToString
@TableName("infra_geo_region")
public class InfraGeoRegionDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    /**
     * Stable division code (e.g. GB/T 2260 style or CN)
     */
    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    /**
     * COUNTRY | PROVINCE | CITY | DISTRICT
     */
    @TableField("level")
    private String level;

    @TableField("parent_id")
    private Long parentId;

    @TableField("sort")
    private Integer sort;
}
