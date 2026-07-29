package com.relayflow.module.calendar.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("cal_all_staff_scope")
public class CalAllStaffScopeDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("calendar_id")
    private Long calendarId;

    /** ALL | USER | DEPT */
    @TableField("scope_type")
    private String scopeType;

    @TableField("scope_id")
    private Long scopeId;
}
