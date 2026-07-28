package com.relayflow.module.infra.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.BaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.OffsetDateTime;
/**
 * <p>
 * China geo sync run log (source + version)
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Getter
@Setter
@ToString
@TableName("infra_geo_sync_meta")
public class InfraGeoSyncMetaDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("source")
    private String source;

    @TableField("data_version")
    private String dataVersion;

    @TableField("status")
    private String status;

    @TableField("region_count")
    private Integer regionCount;

    @TableField("message")
    private String message;

    @TableField("synced_at")
    private OffsetDateTime syncedAt;
}
