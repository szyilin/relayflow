package com.relayflow.module.calendar.controller.admin.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalProductSettingsUpdateReqVO {

    @NotNull
    private Boolean allowMemberCreateOwnedCalendar;
}
