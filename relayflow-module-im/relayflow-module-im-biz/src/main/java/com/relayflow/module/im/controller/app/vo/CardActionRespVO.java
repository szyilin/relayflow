package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class CardActionRespVO {

    private CardToastRespVO toast;
    private MessageItemRespVO message;

    @Data
    public static class CardToastRespVO {
        private String type;
        private String content;
    }
}
