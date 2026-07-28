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
@TableName("mtg_participant")
public class MtgParticipantDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("meeting_id")
    private Long meetingId;

    @TableField("user_id")
    private Long userId;

    @TableField("role")
    private String role;

    @TableField("joined_at")
    private OffsetDateTime joinedAt;

    @TableField("left_at")
    private OffsetDateTime leftAt;
}
