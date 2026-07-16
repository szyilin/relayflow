package com.relayflow.module.im.api.bot.dto.card;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** Card document for {@link com.relayflow.module.im.api.bot.dto.ImBotSendCommand}. */
@Data
public class ImBotCardDocument {

    private Integer schema = 1;
    private String cardId;
    /** V1: {@code generic.v1} */
    private String template;
    private ImBotCardHeader header;
    private List<ImBotCardField> fields;
    private List<ImBotCardAction> actions;
    private ImBotCardMeta meta;

    @Data
    public static class ImBotCardHeader {
        private String title;
        private String subtitle;
    }

    @Data
    public static class ImBotCardField {
        private String label;
        private String value;
    }

    @Data
    public static class ImBotCardAction {
        private String id;
        private String label;
        /** {@code default} | {@code primary} | {@code danger} */
        private String style;
        private ImBotCardBehavior behavior;
    }

    @Data
    public static class ImBotCardBehavior {
        /** {@code open_url} | {@code callback} */
        private String type;
        private String route;
        private String actionKey;
        private Map<String, Object> payload;
        private List<ImBotCardFormField> form;
    }

    @Data
    public static class ImBotCardFormField {
        private String name;
        private String label;
        private Boolean required;
        /** {@code text} | {@code textarea} */
        private String control;
    }

    @Data
    public static class ImBotCardMeta {
        /** ISO-8601 instant; after this, callbacks are rejected. */
        private String expiresAt;
        private ImBotCardSource source;
    }

    @Data
    public static class ImBotCardSource {
        private String domain;
        private String entityType;
        private String entityId;
    }
}
