package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

@Data
public class AppContactItemRespVO {

    private Long id;
    private String nickname;
    private String username;
    private Long deptId;
    private String deptName;
    private String avatarText;
    /** Tenant-member or user avatar file id / URL; null when unset. */
    private String avatar;
}
