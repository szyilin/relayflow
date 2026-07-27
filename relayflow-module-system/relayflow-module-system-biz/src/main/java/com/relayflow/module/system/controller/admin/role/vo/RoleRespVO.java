package com.relayflow.module.system.controller.admin.role.vo;

import com.relayflow.module.system.enums.RoleType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class RoleRespVO {

    private Long id;
    private Long parentId;
    private String name;
    /** System roles only (e.g. super_admin); custom roles are null */
    private String code;
    private RoleType roleType;
    private Integer canDelegate;
    private Integer sort;
    /** 0=启用 1=停用 */
    private Integer status;
    private String remark;
    private OffsetDateTime createTime;
    private List<Long> permissionIds;
    private String permissionSummary;
    private List<RoleAdminSimpleVO> admins = new ArrayList<>();
}
