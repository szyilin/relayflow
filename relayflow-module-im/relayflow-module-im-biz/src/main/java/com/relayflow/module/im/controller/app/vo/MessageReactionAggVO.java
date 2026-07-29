package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class MessageReactionAggVO {

    private String emoji;
    private Integer count;
    private Boolean me;
}
