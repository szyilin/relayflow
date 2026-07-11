package com.relayflow.module.infra.controller.admin.file.vo;

import lombok.Data;

@Data
public class FileUploadConfirmRespVO {

    private Long fileId;
    private String storageUri;
}
