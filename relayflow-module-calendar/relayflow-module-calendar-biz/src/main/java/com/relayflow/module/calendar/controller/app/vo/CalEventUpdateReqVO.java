package com.relayflow.module.calendar.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class CalEventUpdateReqVO extends CalEventCreateReqVO {

    @NotNull
    private Long id;

    /** THIS | ALL | THIS_AND_FUTURE */
    private String editScope;

    private OffsetDateTime instanceStart;
}
