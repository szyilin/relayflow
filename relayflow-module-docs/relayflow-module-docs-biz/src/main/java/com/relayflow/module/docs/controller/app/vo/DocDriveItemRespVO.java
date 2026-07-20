package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocDriveItemRespVO {

    private Long itemId;
    private Long folderId;
    private Long objectId;
    private String type;
    private String title;
    private Long storageFileId;
    private Long sizeBytes;
    private String mimeType;
    private Integer sortOrder;
    private OffsetDateTime updateTime;
}
