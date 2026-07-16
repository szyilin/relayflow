package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Enterprise-scoped user workspace preferences.
 *
 * @author relayflow-codegen
 * @since 2026-07-16
 */
@Getter
@Setter
@ToString
@TableName("sys_user_preference")
public class SysUserPreferenceDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField(value = "settings", typeHandler = JsonbTypeHandler.class)
    private String settings;

    @TableField("schema_version")
    private Integer schemaVersion;
}
