package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocStarredItemRespVO {

    private Long objectId;

    private String title;

    private String type;

    private OffsetDateTime starredAt;

    private String accessState;

    private Long ownerUserId;

    private String ownerDisplayName;

    private String ownerAvatarFileId;

    private Boolean ownedByMe;

    private String locationLabel;

    private OffsetDateTime createTime;

    private OffsetDateTime lastOpenedAt;
}
