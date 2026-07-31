package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class ImUserTagItemRespVO {

    private Long id;
    private String name;
    /** How many conversations currently have this tag (current user). */
    private Integer conversationCount;
}
