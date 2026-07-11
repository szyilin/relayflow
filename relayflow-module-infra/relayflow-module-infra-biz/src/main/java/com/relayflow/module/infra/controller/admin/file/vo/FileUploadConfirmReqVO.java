package com.relayflow.module.infra.controller.admin.file.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileUploadConfirmReqVO {

    @NotNull
    private Long uploadId;

    private String etag;

    @NotNull
    @Min(1)
    private Long size;
}
