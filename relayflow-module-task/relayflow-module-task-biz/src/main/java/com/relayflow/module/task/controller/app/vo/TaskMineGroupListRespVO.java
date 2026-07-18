package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskMineGroupListRespVO {

    private List<TaskMineGroupRespVO> groups = new ArrayList<>();

    private List<TaskMineGroupMembershipVO> memberships = new ArrayList<>();
}
