package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocRecentItemRespVO {

    private Long objectId;

    private String title;

    private String titleSnapshot;

    private String type;

    private String accessState;

    private String locationLabel;

    private Long ownerUserId;

    private String ownerDisplayName;

    private Boolean ownedByMe;

    private OffsetDateTime createTime;

    private OffsetDateTime lastOpenedAt;

    private Boolean starred;
}
