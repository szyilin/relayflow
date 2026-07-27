package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocShareItemRespVO {

    private Long shareId;

    private Long granteeUserId;

    private String granteeDisplayName;

    private String permission;

    private Long grantedByUserId;

    private OffsetDateTime createTime;
}
