package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class AppContactAccountSearchHitRespVO {

    private Long userId;

    private String displayName;

    private String orgLabel;

    private String avatarText;

    private Boolean alreadyAdded;
}
