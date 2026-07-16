package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class GroupMemberItemRespVO {

    /** {@code user} | {@code bot} */
    private String subjectType;

    /** Present when {@code subjectType=user}. */
    private Long userId;

    /** Present when {@code subjectType=bot}. */
    private Long botId;

    /** Present when {@code subjectType=bot}. */
    private String botCode;

    private String nickname;
    private String avatarText;
    private String role;
}
