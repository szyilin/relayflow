package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class StickerPackRespVO {

    private Long id;
    private String code;
    private String name;
    private String scope;
    private List<StickerItemRespVO> stickers;
}
