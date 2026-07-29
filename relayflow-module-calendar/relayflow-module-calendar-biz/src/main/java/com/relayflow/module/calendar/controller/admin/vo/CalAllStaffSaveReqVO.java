package com.relayflow.module.calendar.controller.admin.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CalAllStaffSaveReqVO {

    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String color;

    private String description;

    @NotEmpty
    @Valid
    private List<CalAllStaffScopeItemVO> scopes = new ArrayList<>();
}
