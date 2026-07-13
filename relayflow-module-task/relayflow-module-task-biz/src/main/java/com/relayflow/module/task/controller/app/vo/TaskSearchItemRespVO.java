package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

@Data
public class TaskSearchItemRespVO {

    private Long id;
    private String title;
    private String subtitle;
    private String route;
    private String entityType;
    private String entityId;
}
