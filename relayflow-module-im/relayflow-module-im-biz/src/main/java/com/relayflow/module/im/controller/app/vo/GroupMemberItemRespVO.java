package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class GroupMemberItemRespVO {

    private Long userId;
    private String nickname;
    private String avatarText;
    private String role;
}
