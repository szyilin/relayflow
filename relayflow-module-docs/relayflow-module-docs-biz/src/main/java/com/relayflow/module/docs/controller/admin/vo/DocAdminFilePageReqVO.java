package com.relayflow.module.docs.controller.admin.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DocAdminFilePageReqVO {

    @Min(1)
    private Integer pageNo = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    private String keyword;

    /** RICH_DOC | FILE */
    private String type;

    /** ACTIVE | DELETED */
    private String lifecycle;

    private Long ownerUserId;
}
