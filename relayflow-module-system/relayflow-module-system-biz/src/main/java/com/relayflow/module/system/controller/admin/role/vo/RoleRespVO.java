package com.relayflow.module.system.controller.admin.role.vo;

import com.relayflow.module.system.enums.DataScope;
import com.relayflow.module.system.enums.RoleType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class RoleRespVO {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private RoleType roleType;
    private DataScope dataScope;
    private Integer canDelegate;
    private Integer sort;
    /** 0=启用 1=停用 */
    private Integer status;
    private String remark;
    private OffsetDateTime createTime;
    private List<Long> permissionIds;
    private List<Long> deptIds;
}
