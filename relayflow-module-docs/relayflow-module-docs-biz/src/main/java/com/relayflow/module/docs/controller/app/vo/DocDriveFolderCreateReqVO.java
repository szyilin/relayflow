package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

@Data
public class DocDriveFolderCreateReqVO {

    private Long parentId;

    private String name;
}
