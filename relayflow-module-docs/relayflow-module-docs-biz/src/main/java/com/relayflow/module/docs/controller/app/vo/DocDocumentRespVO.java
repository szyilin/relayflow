package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class DocDocumentRespVO {

    private Long objectId;

    private String title;

    private String type;

    private Map<String, Object> body;

    private String bodyFormat;

    private Integer contentVersion;

    private OffsetDateTime lastOpenedAt;
}
