package com.relayflow.module.docs.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@TableName("doc_storage_quota_dept_rule")
public class DocStorageQuotaDeptRuleDO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("dept_id")
    private Long deptId;

    /** null = unlimited for members under this primary dept (when no user rule) */
    @TableField("quota_bytes")
    private Long quotaBytes;

    private Long creator;

    private OffsetDateTime createTime;

    private Long updater;

    private OffsetDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
