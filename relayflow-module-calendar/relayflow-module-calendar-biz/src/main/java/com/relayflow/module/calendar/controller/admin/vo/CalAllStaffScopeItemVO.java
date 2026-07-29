package com.relayflow.module.calendar.controller.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CalAllStaffScopeItemVO {

    /** ALL | USER | DEPT */
    @NotBlank
    private String scopeType;

    /** Required when USER/DEPT */
    private Long scopeId;

    private String scopeLabel;
}
