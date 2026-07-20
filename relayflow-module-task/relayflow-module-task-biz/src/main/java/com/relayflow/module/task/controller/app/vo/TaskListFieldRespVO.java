package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class TaskListFieldRespVO {

    private Long id;

    private Long listId;

    private String name;

    private String fieldKey;

    private String fieldType;

    private Integer rank;

    private List<TaskListFieldOptionRespVO> options;
}
