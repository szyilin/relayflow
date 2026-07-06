package com.relayflow.module.system.controller.admin.permission.vo;

import lombok.Data;

import java.util.List;

@Data
public class PermissionRespVO {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private Integer type;
    private Integer sort;
    private List<PermissionRespVO> children;
}
