package com.relayflow.module.system.controller.admin.dept.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DeptRespVO {

    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private Long leaderUserId;
    /** 0=启用 1=停用 */
    private Integer status;
    private OffsetDateTime createTime;
}
