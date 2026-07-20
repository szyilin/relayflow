package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

@Data
public class DocLibraryNodeSummaryRespVO {

    private Long nodeId;

    private Long objectId;

    private Long parentId;

    private String title;

    private Integer sortOrder;

    private Integer contentVersion;

    private String bodyFormat;
}
