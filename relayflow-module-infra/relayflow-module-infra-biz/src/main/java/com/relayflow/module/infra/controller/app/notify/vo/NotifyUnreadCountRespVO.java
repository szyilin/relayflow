package com.relayflow.module.infra.controller.app.notify.vo;

import lombok.Data;

import java.util.Map;

@Data
public class NotifyUnreadCountRespVO {

    private long unreadCount;

    /** Unread count grouped by notification type; omitted when empty. */
    private Map<String, Long> byType;
}
