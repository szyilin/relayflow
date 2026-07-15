package com.relayflow.module.infra.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.OffsetDateTime;
/**
 * <p>
 * 
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-15
 */
@Getter
@Setter
@ToString
@TableName("infra_file_upload_session")
public class InfraFileUploadSessionDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("status")
    private String status;

    @TableField("provider")
    private String provider;

    @TableField("object_key")
    private String objectKey;

    @TableField("original_name")
    private String originalName;

    @TableField("mime_type")
    private String mimeType;

    @TableField("size")
    private Long size;

    @TableField("access_level")
    private String accessLevel;

    @TableField("expires_at")
    private OffsetDateTime expiresAt;
}
