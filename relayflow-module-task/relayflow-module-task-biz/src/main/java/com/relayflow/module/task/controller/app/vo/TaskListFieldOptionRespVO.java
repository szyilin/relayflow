package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class TaskListFieldOptionRespVO {

    private Long id;

    private String valueKey;

    private String label;

    private Integer rank;

    private String color;
}
