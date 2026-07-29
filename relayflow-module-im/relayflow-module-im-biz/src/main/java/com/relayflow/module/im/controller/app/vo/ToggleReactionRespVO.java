package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class ToggleReactionRespVO {

    private Long messageId;
    private Long conversationId;
    private List<MessageReactionAggVO> reactions;
}
