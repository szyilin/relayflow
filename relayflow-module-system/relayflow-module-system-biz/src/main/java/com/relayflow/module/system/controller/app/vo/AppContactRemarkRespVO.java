package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class AppContactRemarkRespVO {

    private Long targetUserId;
    private String remarkName;
    private String description;
}
