package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocRecentItemRespVO {

    private Long objectId;

    private String title;

    private OffsetDateTime lastOpenedAt;
}
