package com.relayflow.module.docs.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocShareCreateReqVO {

    @NotNull
    private Long granteeUserId;

    /** VIEW | EDIT */
    private String permission;
}
