package com.relayflow.module.calendar.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CalEventUpdateReqVO extends CalEventCreateReqVO {

    @NotNull
    private Long id;
}
