package com.relayflow.module.system.controller.admin.role.vo;

import com.relayflow.module.system.enums.DataScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleCreateReqVO {

    @NotNull(message = "上级角色不能为空")
    private Long parentId;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    @NotNull(message = "数据范围不能为空")
    private DataScope dataScope;

    private Integer canDelegate;
    private Integer sort;
    private Integer status;
    private String remark;
    private List<Long> permissionIds;
    private List<Long> deptIds;
}
