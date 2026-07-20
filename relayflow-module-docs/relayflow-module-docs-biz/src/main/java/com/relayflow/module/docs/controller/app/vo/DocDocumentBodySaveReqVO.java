package com.relayflow.module.docs.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocDocumentBodySaveReqVO {

    @NotNull
    private Object body;

    @NotNull
    private Integer contentVersion;
}
