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
@TableName("cal_attendee")
public class CalAttendeeDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("event_id")
    private Long eventId;

    @TableField("user_id")
    private Long userId;

    @TableField("role")
    private String role;

    @TableField("response")
    private String response;
}
