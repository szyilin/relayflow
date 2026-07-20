package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocDriveFolderRespVO {

    private Long folderId;
    private Long parentId;
    private String name;
    private Integer sortOrder;
    private OffsetDateTime updateTime;
}
