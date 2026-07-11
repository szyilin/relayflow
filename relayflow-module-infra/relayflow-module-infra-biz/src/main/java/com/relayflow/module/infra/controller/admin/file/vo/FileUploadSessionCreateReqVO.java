package com.relayflow.module.infra.controller.admin.file.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileUploadSessionCreateReqVO {

    @NotBlank
    private String filename;

    @NotNull
    @Min(1)
    private Long size;

    @NotBlank
    private String mimeType;

    private String accessLevel;
}
