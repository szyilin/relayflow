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
@TableName("infra_file_binding")
public class InfraFileBindingDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("file_id")
    private Long fileId;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private Long bizId;
}
