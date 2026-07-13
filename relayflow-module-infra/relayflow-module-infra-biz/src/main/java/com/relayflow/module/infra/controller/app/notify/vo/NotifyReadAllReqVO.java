package com.relayflow.module.infra.controller.app.notify.vo;

import lombok.Data;

@Data
public class NotifyReadAllReqVO {

    /** Optional filter; null marks all types read. */
    private String type;
}
