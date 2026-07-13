package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class ConversationSearchItemRespVO {

    private Long id;
    private String title;
    private String subtitle;
    private String route;
    private String entityType;
    private String entityId;
}
