package com.relayflow.module.infra.api.notify.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NotifyItemCommand {

    private Long tenantId;
    private Long userId;
    private String mobile;
    private String type;
    private String title;
    private String body;
    private String dedupeKey;
    private Map<String, Object> payload;
}
