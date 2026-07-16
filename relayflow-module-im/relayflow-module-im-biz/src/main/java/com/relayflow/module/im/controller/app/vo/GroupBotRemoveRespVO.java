package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class GroupBotRemoveRespVO {

    /** {@code true} when an existing bot membership was removed. */
    private boolean removed;
}
