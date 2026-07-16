package com.relayflow.module.system.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class AppUserPreferenceUpdateReqVO {

    private Integer schemaVersion;

    @NotNull
    private Map<String, Object> settings;
}
