package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class TaskListFieldListRespVO {

    private List<TaskListFieldRespVO> fields;

    private List<TaskListFieldValueRespVO> values;
}
