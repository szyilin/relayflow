package com.relayflow.module.docs.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocShareUpdateReqVO {

    @NotBlank
    private String permission;
}
