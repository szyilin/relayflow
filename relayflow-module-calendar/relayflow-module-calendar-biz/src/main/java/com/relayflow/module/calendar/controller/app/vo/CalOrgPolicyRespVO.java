package com.relayflow.module.calendar.controller.app.vo;

import lombok.Data;

@Data
public class CalOrgPolicyRespVO {

    /** Whether members may create OWNED calendars (org product settings). */
    private Boolean allowMemberCreateOwnedCalendar = true;
}
