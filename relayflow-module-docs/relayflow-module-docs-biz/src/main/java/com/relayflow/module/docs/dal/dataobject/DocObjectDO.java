package com.relayflow.module.docs.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
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
 * @since 2026-07-20
 */
@Getter
@Setter
@ToString
@TableName("doc_object")
public class DocObjectDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("type")
    private String type;

    @TableField("title")
    private String title;

    @TableField(value = "body", typeHandler = JsonbTypeHandler.class)
    private String body;

    @TableField("body_format")
    private String bodyFormat;

    @TableField("content_version")
    private Integer contentVersion;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("last_opened_at")
    private OffsetDateTime lastOpenedAt;

    @TableField("storage_file_id")
    private Long storageFileId;
}
