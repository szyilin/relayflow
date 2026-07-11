package com.relayflow.module.infra.controller.admin.file.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FilePageReqVO {

    @Min(1)
    private Integer pageNo = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    private String keyword;
}
