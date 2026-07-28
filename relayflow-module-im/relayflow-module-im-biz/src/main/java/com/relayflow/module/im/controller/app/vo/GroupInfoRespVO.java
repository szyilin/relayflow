package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class GroupInfoRespVO {

    private Long conversationId;
    private String name;
    private Integer memberCount;
    private String myRole;
    private Boolean searchable;
    private String groupKind;
    private Long deptId;
}
