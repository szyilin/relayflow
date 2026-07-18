package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

@Data
public class TaskListGroupRespVO {

    private Long id;

    private Long listId;

    private String name;

    private Integer rank;

    private Boolean isDefault;
}
