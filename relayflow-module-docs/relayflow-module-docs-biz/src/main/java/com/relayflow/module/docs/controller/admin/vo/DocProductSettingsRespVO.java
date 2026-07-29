package com.relayflow.module.docs.controller.admin.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocProductSettingsRespVO {

    @NotNull
    private Boolean memberShareEnabled;

    @NotNull
    private Boolean markdownExportEnabled;

    @NotNull
    private Boolean driveUploadEnabled;
}
