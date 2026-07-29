package com.relayflow.module.calendar.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@TableName("cal_tenant_settings")
public class CalTenantSettingsDO {

    @TableId(type = IdType.INPUT)
    private Long tenantId;

    /** 1 = allow, 0 = deny */
    @TableField("allow_member_create_owned_calendar")
    private Integer allowMemberCreateOwnedCalendar;

    private Long creator;

    private OffsetDateTime createTime;

    private Long updater;

    private OffsetDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
