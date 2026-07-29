package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class UserStickerItemRespVO {

    private Long id;
    private Long fileId;
    private String name;
    private String downloadUrl;
    private Integer sort;
}
