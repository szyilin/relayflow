package com.relayflow.module.system.controller.admin.role.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleUpdateReqVO {

    @NotNull(message = "角色编号不能为空")
    private Long id;

    /** Optional; null / 0 = top-level role */
    private Long parentId;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    private Integer canDelegate;
    private Integer sort;
    private Integer status;
    private String remark;
    private List<Long> permissionIds;
}
