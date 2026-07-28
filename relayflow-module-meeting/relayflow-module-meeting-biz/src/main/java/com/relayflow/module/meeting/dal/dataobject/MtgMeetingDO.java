package com.relayflow.module.meeting.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@TableName("mtg_meeting")
public class MtgMeetingDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("meeting_code")
    private String meetingCode;

    @TableField("title")
    private String title;

    @TableField("host_user_id")
    private Long hostUserId;

    @TableField("status")
    private String status;

    @TableField("scheduled_start")
    private OffsetDateTime scheduledStart;

    @TableField("scheduled_end")
    private OffsetDateTime scheduledEnd;

    @TableField("rtc_session_id")
    private Long rtcSessionId;

    @TableField("calendar_event_id")
    private Long calendarEventId;
}
