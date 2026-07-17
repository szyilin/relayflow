package com.relayflow.module.calendar.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CalEventRescheduleReqVO {

    @NotNull
    private Long id;

    @NotNull
    private OffsetDateTime startTime;

    @NotNull
    private OffsetDateTime endTime;

    /** THIS | ALL — default ALL for non-recurring; THIS for recurring instance */
    private String editScope;

    /** Original instance start when editing a recurring occurrence */
    private OffsetDateTime instanceStart;
}
