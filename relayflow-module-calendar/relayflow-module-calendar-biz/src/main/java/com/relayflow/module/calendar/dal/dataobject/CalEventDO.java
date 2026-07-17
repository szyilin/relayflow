package com.relayflow.module.calendar.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.OffsetDateTime;
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
@TableName("cal_event")
public class CalEventDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("calendar_id")
    private Long calendarId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("start_time")
    private OffsetDateTime startTime;

    @TableField("end_time")
    private OffsetDateTime endTime;

    @TableField("all_day")
    private Integer allDay;

    @TableField("organizer_id")
    private Long organizerId;

    @TableField("remind_before_minutes")
    private Integer remindBeforeMinutes;

    @TableField("all_day_remind_time")
    private String allDayRemindTime;

    @TableField("status")
    private String status;
}
