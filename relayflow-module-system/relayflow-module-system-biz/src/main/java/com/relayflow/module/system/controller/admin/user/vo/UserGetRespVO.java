package com.relayflow.module.system.controller.admin.user.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class UserGetRespVO {

    private Long id;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    /** 0=启用 1=禁用（租户成员状态） */
    private Integer status;
    private Long deptId;
    private List<Long> roleIds = Collections.emptyList();
    private OffsetDateTime createTime;
}
