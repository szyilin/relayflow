package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class StickerItemRespVO {

    private Long id;
    private Long packId;
    private String code;
    private String name;
    private String downloadUrl;
    private String emoji;
}
