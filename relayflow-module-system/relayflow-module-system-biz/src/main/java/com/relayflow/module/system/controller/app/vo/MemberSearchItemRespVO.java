package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class MemberSearchItemRespVO {

    private Long id;
    private String title;
    private String subtitle;
    private String route;
    private String entityType;
    private String entityId;
    private Long deptId;
}
