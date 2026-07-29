package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddUserStickerReqVO {

    @NotNull
    private Long fileId;

    private String name;
}
