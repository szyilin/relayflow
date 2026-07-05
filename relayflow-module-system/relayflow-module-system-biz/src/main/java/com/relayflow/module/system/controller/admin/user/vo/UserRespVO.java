package com.relayflow.module.system.controller.admin.user.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UserRespVO {

    private Long id;
    private String username;
    private String nickname;
    private String dept;
    /** 0=启用 1=禁用（租户成员状态） */
    private Integer status;
    private OffsetDateTime createTime;
}
