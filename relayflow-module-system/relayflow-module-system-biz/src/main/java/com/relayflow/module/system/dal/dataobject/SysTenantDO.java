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
 * @since 2026-07-28
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

    /**
     * Enterprise industry code
     */
    @TableField("industry")
    private String industry;

    /**
     * Enterprise scale code
     */
    @TableField("scale")
    private String scale;

    /**
     * Enterprise contact name
     */
    @TableField("contact_name")
    private String contactName;

    /**
     * Enterprise contact email
     */
    @TableField("contact_email")
    private String contactEmail;

    /**
     * Enterprise contact mobile
     */
    @TableField("contact_mobile")
    private String contactMobile;

    /**
     * Enterprise contact landline
     */
    @TableField("contact_landline")
    private String contactLandline;
}
