package com.relayflow.module.system.controller.admin.dept.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeptCreateReqVO {

    @NotNull(message = "上级部门不能为空")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String name;

    private Integer sort;
    private Long leaderUserId;
    private Integer status;
}
