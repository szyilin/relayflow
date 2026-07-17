package com.relayflow.module.calendar.dal.dataobject;

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
 * @since 2026-07-17
 */
@Getter
@Setter
@ToString
@TableName("cal_calendar")
public class CalCalendarDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("name")
    private String name;

    @TableField("color")
    private String color;

    @TableField("description")
    private String description;

    @TableField("type")
    private String type;
}
