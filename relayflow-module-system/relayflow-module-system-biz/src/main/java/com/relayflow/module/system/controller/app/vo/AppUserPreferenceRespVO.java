package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

import java.util.Map;

@Data
public class AppUserPreferenceRespVO {

    private Integer schemaVersion;

    private Map<String, Object> settings;
}
