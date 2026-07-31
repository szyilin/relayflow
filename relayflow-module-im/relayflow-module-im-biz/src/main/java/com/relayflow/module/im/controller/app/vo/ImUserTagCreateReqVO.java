package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ImUserTagCreateReqVO {

    @NotBlank
    @Size(max = 64)
    private String name;
}
