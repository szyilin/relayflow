package com.relayflow.module.rtc.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("rtc_session")
public class RtcSessionDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("purpose")
    private String purpose;

    @TableField("owner_type")
    private String ownerType;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("room_name")
    private String roomName;

    @TableField("status")
    private String status;

    @TableField("participant_count")
    private Integer participantCount;
}
