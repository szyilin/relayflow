package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class GroupBotCatalogItemRespVO {

    private Long botId;
    private String botCode;
    private String name;
    private String avatarText;
    private boolean alreadyMember;
}
