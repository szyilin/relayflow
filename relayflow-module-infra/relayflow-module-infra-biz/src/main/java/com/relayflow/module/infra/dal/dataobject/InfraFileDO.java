package com.relayflow.module.infra.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
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
@TableName("infra_file")
public class InfraFileDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("provider")
    private String provider;

    @TableField("storage_uri")
    private String storageUri;

    @TableField("object_key")
    private String objectKey;

    @TableField("original_name")
    private String originalName;

    @TableField("mime_type")
    private String mimeType;

    @TableField("size")
    private Long size;

    @TableField("sha256")
    private String sha256;

    @TableField("access_level")
    private String accessLevel;
}
