package com.relayflow.module.calendar.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CalCalendarCreateReqVO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 32)
    private String color;

    @Size(max = 400)
    private String description;
}
