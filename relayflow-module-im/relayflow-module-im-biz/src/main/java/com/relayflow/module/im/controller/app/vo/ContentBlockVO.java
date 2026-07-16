package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class ContentBlockVO {

    private String type;
    private String text;
    private String fileId;
    private String filename;
    private String mimeType;
    private Long size;
    private String downloadUrl;

    /** Deep-link metadata for {@code type=deeplink} (Bot business reach). */
    private String route;
    private String entityType;
    private String entityId;
}
