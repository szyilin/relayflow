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
@TableName("doc_tenant_settings")
public class DocTenantSettingsDO {

    @TableId(value = "tenant_id", type = IdType.INPUT)
    private Long tenantId;

    @TableField("member_share_enabled")
    private Boolean memberShareEnabled;

    @TableField("markdown_export_enabled")
    private Boolean markdownExportEnabled;

    @TableField("drive_upload_enabled")
    private Boolean driveUploadEnabled;

    /** null = unlimited */
    @TableField("storage_quota_default_bytes")
    private Long storageQuotaDefaultBytes;

    private Long creator;

    private OffsetDateTime createTime;

    private Long updater;

    private OffsetDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
