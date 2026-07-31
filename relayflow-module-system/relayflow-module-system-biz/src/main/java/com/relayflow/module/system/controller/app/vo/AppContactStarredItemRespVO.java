package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class AppContactStarredItemRespVO {

    private Long id;

    private Long userId;

    private String displayName;

    private String orgLabel;

    private String avatarText;

    private String avatar;

    private String username;
}
