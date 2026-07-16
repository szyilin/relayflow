package com.relayflow.module.im.dal.dataobject;

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
 * @since 2026-07-16
 */
@Getter
@Setter
@ToString
@TableName("im_bot_tenant_enablement")
public class ImBotTenantEnablementDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("bot_id")
    private Long botId;

    @TableField("enabled")
    private Integer enabled;
}
