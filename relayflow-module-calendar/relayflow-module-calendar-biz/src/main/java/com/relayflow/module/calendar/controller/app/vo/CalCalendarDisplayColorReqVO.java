package com.relayflow.module.calendar.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalCalendarDisplayColorReqVO {

    @NotNull
    private Long id;

    /** Null or blank clears the personal override */
    private String displayColor;
}
