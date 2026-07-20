package com.relayflow.module.docs.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocDriveFileRegisterReqVO {

    private Long folderId;

    @NotNull
    private Long fileId;

    private String title;
}
