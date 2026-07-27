package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocSharedItemRespVO {

    private Long objectId;

    private String title;

    private String type;

    private String permission;

    private Long ownerUserId;

    private String ownerDisplayName;

    private Long grantedByUserId;

    private String grantedByDisplayName;

    private OffsetDateTime sharedAt;

    private Boolean starred;
}
