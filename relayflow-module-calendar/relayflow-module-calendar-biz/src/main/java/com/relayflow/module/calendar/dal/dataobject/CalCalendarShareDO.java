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
@TableName("cal_calendar_share")
public class CalCalendarShareDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("calendar_id")
    private Long calendarId;

    @TableField("grantee_user_id")
    private Long granteeUserId;

    /** READ */
    @TableField("permission")
    private String permission;
}
