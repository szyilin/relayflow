package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AppContactRequestItemRespVO {

    private Long id;

    private Long fromUserId;

    private String displayName;

    private String subtitle;

    private String avatarText;

    /** pending | accepted | rejected */
    private String status;

    private OffsetDateTime createTime;
}
