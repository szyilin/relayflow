package com.relayflow.module.infra.controller.app.notify.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class NotifyItemRespVO {

    private Long id;
    private Long tenantId;
    private String type;
    private String title;
    private String body;
    private Map<String, Object> payload;
    private Boolean read;
    private OffsetDateTime createTime;
}
