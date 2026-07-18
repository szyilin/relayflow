package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TaskViewConfigVO {

    private String displayMode;

    /** null | Map with mode/fieldKey | for JSON flexibility */
    private Object groupBy;

    /** "MANUAL" or { key, order } */
    private Object sort;

    private List<Map<String, Object>> filters;

    private List<String> visibleFieldKeys;
}
