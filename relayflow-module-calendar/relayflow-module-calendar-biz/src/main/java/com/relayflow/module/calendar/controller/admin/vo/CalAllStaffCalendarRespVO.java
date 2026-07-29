package com.relayflow.module.calendar.controller.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CalAllStaffCalendarRespVO {

    private Long id;

    private String name;

    private String color;

    private String description;

    private Long ownerUserId;

    private List<CalAllStaffScopeItemVO> scopes = new ArrayList<>();
}
