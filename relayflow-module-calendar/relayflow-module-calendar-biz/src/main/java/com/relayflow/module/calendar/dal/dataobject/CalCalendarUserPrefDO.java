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
@TableName("cal_calendar_user_pref")
public class CalCalendarUserPrefDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField("calendar_id")
    private Long calendarId;

    /** Null = use calendar default color */
    @TableField("display_color")
    private String displayColor;
}
