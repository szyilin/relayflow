package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

@Data
public class DocEffectiveSettingsRespVO {

    private Boolean memberShareEnabled;

    private Boolean markdownExportEnabled;

    private Boolean driveUploadEnabled;
}
