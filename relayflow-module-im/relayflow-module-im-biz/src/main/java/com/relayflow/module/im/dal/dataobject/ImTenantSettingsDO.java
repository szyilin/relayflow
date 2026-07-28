package com.relayflow.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@TableName("im_tenant_settings")
public class ImTenantSettingsDO {

    @TableId(value = "tenant_id", type = IdType.INPUT)
    private Long tenantId;

    @TableField("group_member_limit_default")
    private Integer groupMemberLimitDefault;

    @TableField("allow_public_group")
    private Boolean allowPublicGroup;

    @TableField("dept_group_leave_policy")
    private String deptGroupLeavePolicy;

    @TableField("member_export_policy")
    private String memberExportPolicy;

    @TableField(value = "member_export_designated_json", typeHandler = JsonbTypeHandler.class)
    private String memberExportDesignatedJson;

    @TableField("recall_enabled")
    private Boolean recallEnabled;

    @TableField("recall_within_hours")
    private Integer recallWithinHours;

    @TableField("edit_enabled")
    private Boolean editEnabled;

    @TableField("edit_within_hours")
    private Integer editWithinHours;

    private Long creator;

    private OffsetDateTime createTime;

    private Long updater;

    private OffsetDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
