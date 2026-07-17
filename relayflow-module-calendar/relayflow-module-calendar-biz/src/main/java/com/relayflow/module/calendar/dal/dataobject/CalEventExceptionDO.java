package com.relayflow.module.calendar.dal.dataobject;

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
@TableName("cal_event_exception")
public class CalEventExceptionDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("master_event_id")
    private Long masterEventId;

    @TableField("original_start")
    private OffsetDateTime originalStart;

    @TableField("cancelled")
    private Integer cancelled;

    @TableField("override_title")
    private String overrideTitle;

    @TableField("override_start")
    private OffsetDateTime overrideStart;

    @TableField("override_end")
    private OffsetDateTime overrideEnd;
}
