package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskListGroupListRespVO {

    private List<TaskListGroupRespVO> groups = new ArrayList<>();

    private List<TaskListGroupMembershipVO> memberships = new ArrayList<>();
}
