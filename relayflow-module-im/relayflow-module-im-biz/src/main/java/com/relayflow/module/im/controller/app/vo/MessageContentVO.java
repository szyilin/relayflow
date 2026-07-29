package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class MessageContentVO {

    private Integer version;
    private List<ContentBlockVO> blocks;
    /** Present when this message was created by forwarding another message. */
    private MessageForwardedFromVO forwardedFrom;
}
