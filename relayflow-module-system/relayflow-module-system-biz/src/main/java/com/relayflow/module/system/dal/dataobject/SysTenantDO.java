package com.relayflow.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.BaseDO;
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
@TableName("sys_tenant")
public class SysTenantDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("status")
    private Integer status;

    @TableField("owner_user_id")
    private Long ownerUserId;
}
