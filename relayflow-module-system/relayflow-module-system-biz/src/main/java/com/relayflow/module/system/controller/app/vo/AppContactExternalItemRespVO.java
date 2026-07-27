package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class AppContactExternalItemRespVO {

    private Long id;

    private Long userId;

    private String displayName;

    private String orgLabel;

    private String avatarText;

    private String sortLetter;
}
