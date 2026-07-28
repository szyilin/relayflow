package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EditMessageReqVO {

    @NotNull
    private Long messageId;

    @NotBlank
    private String content;
}
