package com.relayflow.module.calendar.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalEventRespondReqVO {

    @NotNull
    private Long id;

    /** ACCEPTED | DECLINED */
    @NotBlank
    private String response;
}
