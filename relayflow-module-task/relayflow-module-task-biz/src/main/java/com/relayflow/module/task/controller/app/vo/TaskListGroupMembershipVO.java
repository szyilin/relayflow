package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

@Data
public class TaskListGroupMembershipVO {

    private Long taskId;

    private Long groupId;

    private Integer rank;
}
